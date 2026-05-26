package com.p2pminecraft.runtime;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Synchronisation des positions de joueurs entre pairs P2P.
 *
 * Outbound : Entity.tick() sur ServerPlayer → broadcast toutes les 500ms
 * Inbound  : handleReceived() → log + stockage (ghost entities en Phase 4)
 */
public class PlayerSyncManager {

    public static final byte TYPE_PLAYER = 0x02;

    private static volatile long lastBroadcast = 0;

    /** Dernières positions connues des pairs : peerId → [x, y, z, yaw, pitch] */
    private static final Map<String, double[]> remotePlayers = new ConcurrentHashMap<>();

    // ── Outbound ──────────────────────────────────────────────────────────────

    /** Appelé depuis Entity.tick() pour chaque entité — filtre sur ServerPlayer. */
    public static void onEntityTick(Object entity) {
        if (!entity.getClass().getName().contains("ServerPlayer")) return;

        long now = System.currentTimeMillis();
        if (now - lastBroadcast < 500) return;
        lastBroadcast = now;

        P2PNetwork net = RuntimeInitializer.getNetwork();
        if (net == null) return;

        try {
            double x     = (double) entity.getClass().getMethod("getX").invoke(entity);
            double y     = (double) entity.getClass().getMethod("getY").invoke(entity);
            double z     = (double) entity.getClass().getMethod("getZ").invoke(entity);
            float  yaw   = (float)  entity.getClass().getMethod("getYRot").invoke(entity);
            float  pitch = (float)  entity.getClass().getMethod("getXRot").invoke(entity);

            ByteBuffer buf = ByteBuffer.allocate(1 + 8 + 8 + 8 + 4 + 4); // 33 bytes
            buf.put(TYPE_PLAYER);
            buf.putDouble(x);
            buf.putDouble(y);
            buf.putDouble(z);
            buf.putFloat(yaw);
            buf.putFloat(pitch);

            net.broadcastData(buf.array());
        } catch (Exception ignored) {}
    }

    // ── Inbound ───────────────────────────────────────────────────────────────

    public static void handleReceived(String fromId, byte[] data) {
        if (data.length < 33) return;
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.get(); // skip type
        double x     = buf.getDouble();
        double y     = buf.getDouble();
        double z     = buf.getDouble();
        float  yaw   = buf.getFloat();
        float  pitch = buf.getFloat();

        remotePlayers.put(fromId, new double[]{x, y, z, yaw, pitch});

        System.out.printf("[P2P] Joueur %s à %.1f,%.1f,%.1f%n",
            fromId.substring(0, Math.min(8, fromId.length())), x, y, z);

        GhostManager.updateRemotePlayer(fromId, x, y, z, yaw, pitch);
    }

    public static Map<String, double[]> getRemotePlayers() {
        return remotePlayers;
    }
}
