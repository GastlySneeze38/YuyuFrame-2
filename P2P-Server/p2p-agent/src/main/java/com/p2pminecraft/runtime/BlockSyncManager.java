package com.p2pminecraft.runtime;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Synchronisation des blocs entre pairs P2P.
 *
 * Outbound : LevelChunk.setBlockState() → notifyBlockChanged() → buffer
 *            → flushPendingChanges() envoie tout en un seul paquet TYPE_BLOCK_BATCH par tick.
 * Inbound  : onDataReceived() → handleReceived() → pending queue
 *            → flushPendingChanges() applique TOUS les blocs reçus sans limite.
 *
 * Protocole :
 *   TYPE_BLOCK       0x01  [int x][int y][int z][short idLen][bytes id]   (legacy)
 *   TYPE_BLOCK_BATCH 0x03  [int count] puis count × entrée ci-dessus
 */
public class BlockSyncManager {

    public static final byte TYPE_BLOCK       = 0x01;
    public static final byte TYPE_BLOCK_BATCH = 0x03; // 0x02 réservé à PlayerSyncManager.TYPE_PLAYER

    static final ThreadLocal<Boolean> RECEIVING = ThreadLocal.withInitial(() -> false);
    static final AtomicReference<Object> levelRef = new AtomicReference<>();

    // Queue inbound (reçus depuis le réseau, appliqués sur le thread serveur)
    static final ConcurrentLinkedQueue<byte[]> pending = new ConcurrentLinkedQueue<>();
    // Buffer outbound (accumulés pendant un tick, envoyés en batch)
    private static final ConcurrentLinkedQueue<byte[]> outbound = new ConcurrentLinkedQueue<>();

    // ── Réflexion cachée ──────────────────────────────────────────────────────

    private static volatile Method getXMethod, getYMethod, getZMethod;
    private static volatile Method getBlockMethod;
    private static volatile Method setBlockMethod;

    // ── Enregistrement du niveau ───────────────────────────────────────────────

    public static Object getStoredLevel() { return levelRef.get(); }

    public static void registerLevel(Object level) {
        if (level != null && MappingsRegistry.isInstance(level, "net/minecraft/server/level/ServerLevel"))
            levelRef.set(level);
    }

    // ── Outbound ───────────────────────────────────────────────────────────────

    public static void notifyBlockChanged(Object pos, Object newState, Object level) {
        if (RECEIVING.get() || pos == null || newState == null || level == null) return;
        if (!MappingsRegistry.isInstance(level, "net/minecraft/server/level/ServerLevel")) return;
        if (RuntimeInitializer.getNetwork() == null) return;

        String blockId = getBlockId(newState);
        if (blockId == null) return;

        int x = getCoord(pos, "getX");
        int y = getCoord(pos, "getY");
        int z = getCoord(pos, "getZ");

        byte[] idBytes = blockId.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(4 + 4 + 4 + 2 + idBytes.length);
        buf.putInt(x).putInt(y).putInt(z);
        buf.putShort((short) idBytes.length).put(idBytes);
        outbound.offer(buf.array());
    }

    /** Envoie tous les blocs outbound accumulés en un seul paquet batch. */
    private static void flushOutbound() {
        List<byte[]> entries = new ArrayList<>();
        byte[] e;
        while ((e = outbound.poll()) != null) entries.add(e);
        if (entries.isEmpty()) return;

        P2PNetwork net = RuntimeInitializer.getNetwork();
        if (net == null) return;

        int size = 1 + 4; // type + count
        for (byte[] en : entries) size += en.length;

        ByteBuffer buf = ByteBuffer.allocate(size);
        buf.put(TYPE_BLOCK_BATCH);
        buf.putInt(entries.size());
        for (byte[] en : entries) buf.put(en);
        net.broadcastData(buf.array());
    }

    // ── Inbound ────────────────────────────────────────────────────────────────

    /** Appelé depuis le thread réseau — enqueue le paquet ou le décompose en entrées atomiques. */
    public static void handleReceived(byte[] data) {
        if (data == null || data.length < 1) return;
        if (data[0] == TYPE_BLOCK_BATCH) {
            ByteBuffer buf = ByteBuffer.wrap(data, 1, data.length - 1);
            if (buf.remaining() < 4) return;
            int count = buf.getInt();
            for (int i = 0; i < count; i++) {
                byte[] entry = readBlockEntry(buf);
                if (entry != null) pending.offer(entry);
            }
        } else {
            pending.offer(data); // TYPE_BLOCK legacy
        }
    }

    /** Lit une entrée block depuis un buffer batch et la remballe en paquet TYPE_BLOCK. */
    private static byte[] readBlockEntry(ByteBuffer buf) {
        if (buf.remaining() < 14) return null; // 4+4+4+2 minimum
        int x = buf.getInt(), y = buf.getInt(), z = buf.getInt();
        short idLen = buf.getShort();
        if (idLen < 1 || buf.remaining() < idLen) return null;
        byte[] idBytes = new byte[idLen];
        buf.get(idBytes);
        ByteBuffer out = ByteBuffer.allocate(1 + 4 + 4 + 4 + 2 + idLen);
        out.put(TYPE_BLOCK).putInt(x).putInt(y).putInt(z).putShort(idLen).put(idBytes);
        return out.array();
    }

    /** Appelé chaque tick serveur : envoie le batch outbound ET applique TOUS les blocs reçus. */
    public static void flushPendingChanges() {
        flushOutbound();
        byte[] data;
        while ((data = pending.poll()) != null) applyBlockChange(data);
    }

    private static void applyBlockChange(byte[] data) {
        if (data.length < 13) return;
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.get(); // skip type byte
        int x = buf.getInt(), y = buf.getInt(), z = buf.getInt();
        if (buf.remaining() < 2) return;
        short idLen = buf.getShort();
        if (idLen < 1 || buf.remaining() < idLen) return;
        byte[] idBytes = new byte[idLen];
        buf.get(idBytes);
        String blockId = new String(idBytes, StandardCharsets.UTF_8);

        Object level = levelRef.get();
        if (level == null) return;
        try {
            Object pos   = createBlockPos(x, y, z);
            Object state = lookupBlockState(blockId);
            if (pos == null || state == null) return;
            RECEIVING.set(true);
            try {
                callSetBlock(level, pos, state, 3);
            } finally {
                RECEIVING.set(false);
            }
        } catch (Exception e) {
            System.err.println("[P2P] Erreur apply block (" + blockId + "): " + e.getMessage());
        }
    }

    // ── Helpers réflexion (avec cache) ────────────────────────────────────────

    static int getCoord(Object pos, String mojangGetter) {
        try {
            Method m = switch (mojangGetter) {
                case "getX" -> {
                    if (getXMethod == null) getXMethod = findVec3iMethod(pos, mojangGetter);
                    yield getXMethod;
                }
                case "getY" -> {
                    if (getYMethod == null) getYMethod = findVec3iMethod(pos, mojangGetter);
                    yield getYMethod;
                }
                default -> {
                    if (getZMethod == null) getZMethod = findVec3iMethod(pos, mojangGetter);
                    yield getZMethod;
                }
            };
            return m != null ? (int) m.invoke(pos) : 0;
        } catch (Exception e) { return 0; }
    }

    private static Method findVec3iMethod(Object pos, String mojangGetter) {
        String obf = MappingsRegistry.getObfMethodName("net/minecraft/core/Vec3i", mojangGetter);
        Class<?> cls = pos.getClass();
        while (cls != null && cls != Object.class) {
            try {
                Method m = cls.getDeclaredMethod(obf);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) { cls = cls.getSuperclass(); }
        }
        return null;
    }

    static String getBlockId(Object blockState) {
        try {
            if (getBlockMethod == null) {
                String name = MappingsRegistry.getObfMethodName(
                    "net/minecraft/world/level/block/state/BlockBehaviour$BlockStateBase", "getBlock");
                getBlockMethod = blockState.getClass().getMethod(name);
            }
            Object block = getBlockMethod.invoke(blockState);
            String s = block.toString();
            if (s.startsWith("Block{") && s.endsWith("}")) return s.substring(6, s.length() - 1);
        } catch (Exception ignored) {}
        return null;
    }

    static Object createBlockPos(int x, int y, int z) throws Exception {
        Class<?> cls = MappingsRegistry.loadClass("net/minecraft/core/BlockPos");
        return cls.getConstructor(int.class, int.class, int.class).newInstance(x, y, z);
    }

    private static volatile Object cachedBlockRegistry = null;

    static Object blockRegistry() throws Exception {
        if (cachedBlockRegistry != null) return cachedBlockRegistry;
        Class<?> c = MappingsRegistry.loadClass("net/minecraft/core/registries/BuiltInRegistries");
        String field = MappingsRegistry.getObfFieldName("net/minecraft/core/registries/BuiltInRegistries", "BLOCK");
        return cachedBlockRegistry = c.getField(field).get(null);
    }

    private static volatile java.util.Map<String, Object> blockStateCache = null;

    static Object lookupBlockState(String blockId) throws Exception {
        java.util.Map<String, Object> cache = blockStateCache;
        if (cache == null) cache = buildBlockStateCache();
        return cache.get(blockId);
    }

    private static synchronized java.util.Map<String, Object> buildBlockStateCache() throws Exception {
        if (blockStateCache != null) return blockStateCache;
        Object registry = blockRegistry();
        String defaultBS = MappingsRegistry.getObfMethodName("net/minecraft/world/level/block/Block", "defaultBlockState");
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        for (Object block : (Iterable<?>) registry) {
            String s = block.toString();
            if (s.startsWith("Block{") && s.endsWith("}")) {
                String id = s.substring(6, s.length() - 1);
                try {
                    Object state = block.getClass().getMethod(defaultBS).invoke(block);
                    if (state != null) map.put(id, state);
                } catch (Exception ignored) {}
            }
        }
        blockStateCache = map;
        System.out.println("[P2P] BlockState cache: " + map.size() + " blocs");
        return map;
    }

    static void callSetBlock(Object level, Object pos, Object state, int flags) throws Exception {
        if (setBlockMethod == null) {
            String name = MappingsRegistry.getObfMethodName("net/minecraft/world/level/LevelWriter", "setBlock");
            for (Method m : level.getClass().getMethods()) {
                if (!m.getName().equals(name) || m.getParameterCount() != 3) continue;
                Class<?>[] p = m.getParameterTypes();
                if (!p[2].equals(int.class) || !p[0].isInstance(pos)) continue;
                setBlockMethod = m;
                break;
            }
            if (setBlockMethod == null)
                throw new NoSuchMethodException("Level.setBlock introuvable");
        }
        setBlockMethod.invoke(level, pos, state, flags);
    }
}
