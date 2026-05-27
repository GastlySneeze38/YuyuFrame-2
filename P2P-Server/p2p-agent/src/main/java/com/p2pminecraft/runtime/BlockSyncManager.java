package com.p2pminecraft.runtime;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Synchronisation des blocs entre pairs P2P.
 *
 * Outbound : LevelChunk.setBlockState() → notifyBlockChanged() → broadcastData()
 * Inbound  : onDataReceived() → handleReceived() [enqueue] → flushPendingChanges() [server thread]
 */
public class BlockSyncManager {

    public static final byte TYPE_BLOCK = 0x01;

    /** Empêche de re-broadcaster les changements reçus du réseau. */
    static final ThreadLocal<Boolean> RECEIVING = ThreadLocal.withInitial(() -> false);

    /** Référence au ServerLevel courant, mise à jour depuis ChunkTransformer.tick(). */
    static final AtomicReference<Object> levelRef = new AtomicReference<>();

    /** File thread-safe : blocs reçus, à appliquer sur le thread serveur. */
    static final ConcurrentLinkedQueue<byte[]> pending = new ConcurrentLinkedQueue<>();

    // ── Enregistrement du niveau (appelé par ChunkTransformer) ────────────────

    public static void registerLevel(Object level) {
        if (level != null && level.getClass().getName().contains("ServerLevel")) {
            levelRef.set(level);
        }
    }

    // ── Outbound : un bloc a changé localement ────────────────────────────────

    public static void notifyBlockChanged(int x, int y, int z, Object newState, Object level) {
        if (RECEIVING.get() || newState == null || level == null) return;
        if (!level.getClass().getName().contains("ServerLevel")) return;

        P2PNetwork net = RuntimeInitializer.getNetwork();
        if (net == null) return;

        String blockId = getBlockId(newState);
        if (blockId == null) return;

        byte[] idBytes = blockId.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(1 + 4 + 4 + 4 + 2 + idBytes.length);
        buf.put(TYPE_BLOCK);
        buf.putInt(x);
        buf.putInt(y);
        buf.putInt(z);
        buf.putShort((short) idBytes.length);
        buf.put(idBytes);

        net.broadcastData(buf.array());
    }

    // ── Inbound : données reçues depuis le thread WebSocket ───────────────────

    public static void handleReceived(byte[] data) {
        pending.offer(data);
    }

    /** Appelé depuis afterChunkTick (thread serveur) — applique les blocs en attente. */
    public static void flushPendingChanges() {
        byte[] data;
        int limit = 64;
        while ((data = pending.poll()) != null && limit-- > 0) {
            applyBlockChange(data);
        }
    }

    private static void applyBlockChange(byte[] data) {
        if (data.length < 13) return;
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.get(); // type
        int x = buf.getInt();
        int y = buf.getInt();
        int z = buf.getInt();
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
                System.out.println("[P2P] Bloc appliqué: " + blockId + " @ " + x + "," + y + "," + z);
            } finally {
                RECEIVING.set(false);
            }
        } catch (Exception e) {
            System.err.println("[P2P] Erreur apply block (" + blockId + "): " + e.getMessage());
        }
    }

    // ── Helpers de réflexion ──────────────────────────────────────────────────

    /** Extrait l'ID du bloc depuis le toString() de Block : "Block{minecraft:stone}" → "minecraft:stone" */
    static String getBlockId(Object blockState) {
        try {
            Method getBlock = blockState.getClass().getMethod("getBlock");
            Object block = getBlock.invoke(blockState);
            String s = block.toString();
            if (s.startsWith("Block{") && s.endsWith("}")) {
                return s.substring(6, s.length() - 1);
            }
        } catch (Exception ignored) {}
        return null;
    }

    static Object createBlockPos(int x, int y, int z) throws Exception {
        Class<?> cls = Class.forName("net.minecraft.core.BlockPos");
        return cls.getConstructor(int.class, int.class, int.class).newInstance(x, y, z);
    }

    private static volatile Object cachedBlockRegistry = null;

    static Object blockRegistry() throws Exception {
        if (cachedBlockRegistry != null) return cachedBlockRegistry;
        Class<?> c = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
        return cachedBlockRegistry = c.getField("BLOCK").get(null);
    }

    static Object parseResourceLocation(String id) throws Exception {
        Class<?> cls = Class.forName("net.minecraft.resources.ResourceLocation");
        try {
            return cls.getMethod("parse", String.class).invoke(null, id);
        } catch (NoSuchMethodException e) {
            return cls.getConstructor(String.class).newInstance(id);
        }
    }

    static Object lookupBlockState(String blockId) throws Exception {
        Object registry = blockRegistry();
        Object rl = parseResourceLocation(blockId);

        for (Method m : registry.getClass().getMethods()) {
            if (!m.getName().equals("get") || m.getParameterCount() != 1) continue;
            if (!m.getParameterTypes()[0].isAssignableFrom(rl.getClass())) continue;
            try {
                Object block = m.invoke(registry, rl);
                if (block == null) continue;
                return block.getClass().getMethod("defaultBlockState").invoke(block);
            } catch (Exception ignored) {}
        }
        return null;
    }

    static void callSetBlock(Object level, Object pos, Object state, int flags) throws Exception {
        for (Method m : level.getClass().getMethods()) {
            if (!m.getName().equals("setBlock") || m.getParameterCount() != 3) continue;
            Class<?>[] params = m.getParameterTypes();
            if (!params[2].equals(int.class)) continue;
            if (!params[0].getName().contains("BlockPos")) continue;
            m.invoke(level, pos, state, flags);
            return;
        }
        throw new NoSuchMethodException("Level.setBlock(BlockPos,BlockState,int) introuvable");
    }
}
