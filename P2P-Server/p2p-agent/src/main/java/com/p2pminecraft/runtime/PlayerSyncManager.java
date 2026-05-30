package com.p2pminecraft.runtime;

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
        if (!MappingsRegistry.isInstance(entity, "net/minecraft/server/level/ServerPlayer")) return;

        long now = System.currentTimeMillis();
        if (now - lastBroadcast < 500) return;
        lastBroadcast = now;

        P2PNetwork net = RuntimeInitializer.getNetwork();
        if (net == null) return;

        try {
            String entityClass = "net/minecraft/world/entity/Entity";
            String getXName   = MappingsRegistry.getObfMethodName(entityClass, "getX");
            String getYName   = MappingsRegistry.getObfMethodName(entityClass, "getY");
            String getZName   = MappingsRegistry.getObfMethodName(entityClass, "getZ");
            String getYRotName = MappingsRegistry.getObfMethodName(entityClass, "getYRot");
            String getXRotName = MappingsRegistry.getObfMethodName(entityClass, "getXRot");

            double x     = (double) entity.getClass().getMethod(getXName).invoke(entity);
            double y     = (double) entity.getClass().getMethod(getYName).invoke(entity);
            double z     = (double) entity.getClass().getMethod(getZName).invoke(entity);
            float  yaw   = (float)  entity.getClass().getMethod(getYRotName).invoke(entity);
            float  pitch = (float)  entity.getClass().getMethod(getXRotName).invoke(entity);

            ByteBuffer buf = ByteBuffer.allocate(1 + 8 + 8 + 8 + 4 + 4);
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

    /**
     * Itère les joueurs du ServerLevel et broadcast leurs positions.
     * Appelé depuis le tick hook (ServerLevelMixin) sur le thread serveur.
     */
    public static void broadcastFromLevel(Object level) {
        try {
            String serverLevelClass = "net/minecraft/server/level/ServerLevel";
            String playersName = MappingsRegistry.getObfMethodName(serverLevelClass, "players");
            java.lang.reflect.Method m = level.getClass().getMethod(playersName);
            java.util.List<?> players = (java.util.List<?>) m.invoke(level);
            if (!players.isEmpty()) {
                Object first = players.get(0);
                // Notifie SnapshotManager de la présence du joueur (position en chunks)
                try {
                    String entityClass = "net/minecraft/world/entity/Entity";
                    String gx = MappingsRegistry.getObfMethodName(entityClass, "getX");
                    String gz = MappingsRegistry.getObfMethodName(entityClass, "getZ");
                    double x = (double) first.getClass().getMethod(gx).invoke(first);
                    double z = (double) first.getClass().getMethod(gz).invoke(first);
                    SnapshotManager.notifyPlayerPresent((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
                } catch (Exception ignored2) {}
            }
            for (Object player : players) {
                onEntityTick(player);
            }
        } catch (Exception ignored) {}
    }

    public static Map<String, double[]> getRemotePlayers() {
        return remotePlayers;
    }
}
