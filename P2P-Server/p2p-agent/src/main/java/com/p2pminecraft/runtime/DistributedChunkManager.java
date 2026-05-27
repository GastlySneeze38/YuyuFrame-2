package com.p2pminecraft.runtime;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Appelé par le bytecode injecté par ChunkTransformer et ServerLevelTransformer.
 * Délègue à RustBridge (JNI) ou au fallback Java si la lib Rust est absente.
 */
public class DistributedChunkManager {

    private static final int VIEW_DISTANCE = 3;

    // Fallback Java : même logique que le Rust
    private static String myId = "default";
    private static int myX = 0, myZ = 0;
    private static final Map<String, int[]> fallbackPeers = new ConcurrentHashMap<>();

    // Stats de monitoring
    private static volatile long chunksSkipped = 0;
    private static volatile long chunksComputed = 0;

    // Broadcast de position périodique (toutes les 2s)
    private static volatile long lastPositionBroadcast = 0;
    private static volatile int  lastBroadcastCx = Integer.MIN_VALUE;
    private static volatile int  lastBroadcastCz = Integer.MIN_VALUE;

    // ---- API appelée par le bytecode injecté ----

    public static boolean shouldTickWorld() {
        // Phase 1 : toujours true (on contrôle au niveau chunk, pas monde)
        return true;
    }

    public static boolean shouldTickChunk(int cx, int cz) {
        boolean result = RustBridge.NATIVE_LOADED
            ? RustBridge.shouldTickChunk(cx, cz)
            : javaFallback(cx, cz);

        if (result) chunksComputed++; else chunksSkipped++;
        return result;
    }

    public static int getMyChunkX() { return myX; }
    public static int getMyChunkZ() { return myZ; }

    public static void afterChunkTick(int cx, int cz) {
        // Applique les blocs reçus + commandes ghost + snapshots (thread serveur)
        BlockSyncManager.flushPendingChanges();
        GhostManager.flush();
        SnapshotManager.flush();

        long now = System.currentTimeMillis();
        if (now - lastPositionBroadcast > 2000) {
            lastPositionBroadcast = now;
            if (cx != lastBroadcastCx || cz != lastBroadcastCz) {
                lastBroadcastCx = cx;
                lastBroadcastCz = cz;
                P2PNetwork net = RuntimeInitializer.getNetwork();
                if (net != null) net.updateMyPosition(cx, cz);
            }
            System.out.println("[P2P] " + stats());
        }
    }

    /** Dispatche les messages data reçus d'un pair selon le type. */
    public static void onDataReceived(String fromId, byte[] data) {
        if (data.length == 0) return;
        byte type = data[0];
        if (type == BlockSyncManager.TYPE_BLOCK) {
            BlockSyncManager.handleReceived(data);
        } else if (type == PlayerSyncManager.TYPE_PLAYER) {
            PlayerSyncManager.handleReceived(fromId, data);
        } else if (type == SnapshotManager.TYPE_SNAPSHOT) {
            SnapshotManager.handleReceived(data);
        } else {
            System.out.println("[P2P] Message inconnu (type=0x" + Integer.toHexString(type & 0xFF)
                + ") de " + fromId.substring(0, Math.min(8, fromId.length())));
        }
    }

    // ---- API appelée par P2PNetwork ----

    public static void init(String peerId, int x, int z) {
        myId = peerId;
        myX = x;
        myZ = z;
        if (RustBridge.NATIVE_LOADED) RustBridge.init(peerId, x, z);
    }

    public static void setMyPosition(int cx, int cz) {
        myX = cx;
        myZ = cz;
        if (RustBridge.NATIVE_LOADED) RustBridge.setMyPosition(cx, cz);
    }

    /** Pair qui vient d'arriver (peer_joined) — lui envoyer le snapshot. */
    public static void upsertPeer(String peerId, int x, int z) {
        boolean isNew = fallbackPeers.put(peerId, new int[]{x, z}) == null;
        if (RustBridge.NATIVE_LOADED) RustBridge.upsertPeer(peerId, x, z);
        if (isNew) SnapshotManager.onNewPeer(peerId);
    }

    /** Pair déjà présent dans la peer_list initiale — ne pas lui envoyer de snapshot. */
    public static void registerExistingPeer(String peerId, int x, int z) {
        fallbackPeers.put(peerId, new int[]{x, z});
        if (RustBridge.NATIVE_LOADED) RustBridge.upsertPeer(peerId, x, z);
        SnapshotManager.markKnown(peerId);
    }

    public static void removePeer(String peerId) {
        fallbackPeers.remove(peerId);
        if (RustBridge.NATIVE_LOADED) RustBridge.removePeer(peerId);
        GhostManager.removeGhost(peerId);
        SnapshotManager.onPeerLeft(peerId);
    }

    public static int myChunkCount() {
        return RustBridge.NATIVE_LOADED ? RustBridge.myChunkCount() : -1;
    }

    public static String stats() {
        return String.format("computed=%d skipped=%d chunks=%d peers=%d",
            chunksComputed, chunksSkipped, myChunkCount(), fallbackPeers.size());
    }

    // ---- Fallback Java (identique à l'algo Rust) ----

    private static boolean javaFallback(int cx, int cz) {
        List<String> nearby = new ArrayList<>();
        for (var e : fallbackPeers.entrySet()) {
            int[] pos = e.getValue();
            if (Math.abs(pos[0] - cx) <= VIEW_DISTANCE && Math.abs(pos[1] - cz) <= VIEW_DISTANCE) {
                nearby.add(e.getKey());
            }
        }
        nearby.add(myId);
        Collections.sort(nearby);

        long hash = Math.abs((long) cx * 73856093L + (long) cz * 19349663L);
        return nearby.get((int) (hash % nearby.size())).equals(myId);
    }
}
