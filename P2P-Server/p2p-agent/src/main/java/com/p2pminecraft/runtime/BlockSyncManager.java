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

    static final ThreadLocal<Boolean> RECEIVING = ThreadLocal.withInitial(() -> false);
    static final AtomicReference<Object> levelRef = new AtomicReference<>();
    static final ConcurrentLinkedQueue<byte[]> pending = new ConcurrentLinkedQueue<>();

    // ── Enregistrement du niveau ───────────────────────────────────────────────

    public static Object getStoredLevel() { return levelRef.get(); }

    public static void registerLevel(Object level) {
        if (level == null) return;
        if (MappingsRegistry.isInstance(level, "net/minecraft/server/level/ServerLevel")) {
            levelRef.set(level);
        }
    }

    // ── Outbound ───────────────────────────────────────────────────────────────

    // pos = Object (BlockPos / is à runtime) — coords extraites par réflexion sur Vec3i (jy).
    // Évite que LevelChunkMixin ait des paramètres typés MC non remappés dans le handler.
    public static void notifyBlockChanged(Object pos, Object newState, Object level) {
        if (RECEIVING.get() || pos == null || newState == null || level == null) return;
        if (!MappingsRegistry.isInstance(level, "net/minecraft/server/level/ServerLevel")) return;

        P2PNetwork net = RuntimeInitializer.getNetwork();
        if (net == null) return;

        String blockId = getBlockId(newState);
        if (blockId == null) return;

        int x = getVec3iCoord(pos, "getX");
        int y = getVec3iCoord(pos, "getY");
        int z = getVec3iCoord(pos, "getZ");

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

    // Vec3i.getX()→u, getY()→v, getZ()→w (client-mappings-1.21.11, Vec3i=jy)
    // BlockPos (is) hérite de Vec3i (jy) → cherche en remontant la hiérarchie.
    static int getVec3iCoord(Object blockPos, String mojangGetter) {
        String obf = MappingsRegistry.getObfMethodName("net/minecraft/core/Vec3i", mojangGetter);
        Class<?> cls = blockPos.getClass();
        while (cls != null && cls != Object.class) {
            try {
                java.lang.reflect.Method m = cls.getDeclaredMethod(obf);
                m.setAccessible(true);
                return (int) m.invoke(blockPos);
            } catch (NoSuchMethodException ignored) {
                cls = cls.getSuperclass();
            } catch (Exception e) {
                break;
            }
        }
        return 0;
    }

    // ── Inbound ────────────────────────────────────────────────────────────────

    public static void handleReceived(byte[] data) {
        pending.offer(data);
    }

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
        buf.get();
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
                System.out.println("[P2P] Bloc appliqué: " + blockId + " @ " + x + "," + y + "," + z);
            } finally {
                RECEIVING.set(false);
            }
        } catch (Exception e) {
            System.err.println("[P2P] Erreur apply block (" + blockId + "): " + e.getMessage());
        }
    }

    // ── Helpers de réflexion ───────────────────────────────────────────────────

    /** "Block{minecraft:stone}" → "minecraft:stone" */
    static String getBlockId(Object blockState) {
        try {
            String getBlockName = MappingsRegistry.getObfMethodName(
                "net/minecraft/world/level/block/state/BlockBehaviour$BlockStateBase", "getBlock");
            Method getBlock = blockState.getClass().getMethod(getBlockName);
            Object block = getBlock.invoke(blockState);
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
        String blockFieldName = MappingsRegistry.getObfFieldName(
            "net/minecraft/core/registries/BuiltInRegistries", "BLOCK");
        return cachedBlockRegistry = c.getField(blockFieldName).get(null);
    }

    static Object parseResourceLocation(String id) throws Exception {
        Class<?> cls = MappingsRegistry.loadClass("net/minecraft/resources/ResourceLocation");
        String parseMethod = MappingsRegistry.getObfMethodName(
            "net/minecraft/resources/ResourceLocation", "parse");
        try {
            return cls.getMethod(parseMethod, String.class).invoke(null, id);
        } catch (NoSuchMethodException e) {
            return cls.getConstructor(String.class).newInstance(id);
        }
    }

    static Object lookupBlockState(String blockId) throws Exception {
        Object registry = blockRegistry();
        Object rl = parseResourceLocation(blockId);

        String getMethodName = MappingsRegistry.getObfMethodName(
            "net/minecraft/core/Registry", "get");
        String defaultBlockStateName = MappingsRegistry.getObfMethodName(
            "net/minecraft/world/level/block/Block", "defaultBlockState");

        for (Method m : registry.getClass().getMethods()) {
            if (!m.getName().equals(getMethodName) || m.getParameterCount() != 1) continue;
            if (!m.getParameterTypes()[0].isInstance(rl)) continue;
            try {
                Object block = m.invoke(registry, rl);
                if (block == null) continue;
                return block.getClass().getMethod(defaultBlockStateName).invoke(block);
            } catch (Exception ignored) {}
        }
        return null;
    }

    static void callSetBlock(Object level, Object pos, Object state, int flags) throws Exception {
        String setBlockName = MappingsRegistry.getObfMethodName(
            "net/minecraft/world/level/LevelWriter", "setBlock");
        for (Method m : level.getClass().getMethods()) {
            if (!m.getName().equals(setBlockName) || m.getParameterCount() != 3) continue;
            Class<?>[] params = m.getParameterTypes();
            if (!params[2].equals(int.class)) continue;
            if (!params[0].isInstance(pos)) continue;
            m.invoke(level, pos, state, flags);
            return;
        }
        throw new NoSuchMethodException("Level.setBlock(BlockPos,BlockState,int) introuvable");
    }
}
