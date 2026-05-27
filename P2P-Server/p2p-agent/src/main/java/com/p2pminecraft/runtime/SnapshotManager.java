package com.p2pminecraft.runtime;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

/**
 * Snapshot initial du monde.
 *
 * Quand un nouveau pair rejoint la session, l'hôte sérialise tous les chunks
 * chargés autour de sa position et les envoie via P2P.
 * Le pair entrant les applique sur son serveur intégré (qui met à jour ClientWorld
 * via le pipeline normal de Minecraft — pas d'injection ClientWorld nécessaire).
 *
 * Format d'un message snapshot (GZIP compressé après le header) :
 *   TYPE(1) + chunkX(4) + chunkZ(4) + blockCount(4) + compressed(...)
 * Chaque bloc dans la partie compressée :
 *   relX(1) + worldY(2) + relZ(1) + idLen(1) + blockId(idLen)
 */
public class SnapshotManager {

    public static final byte TYPE_SNAPSHOT = 0x03;

    /** Rayon de snapshot en chunks (5x5 = 25 chunks). */
    private static final int RADIUS = 2;

    /** Pairs ayant déjà reçu un snapshot cette session. */
    private static final Set<String> sent = ConcurrentHashMap.newKeySet();

    /** File d'envois à traiter sur le thread serveur. */
    private static final ConcurrentLinkedQueue<String> sendQueue = new ConcurrentLinkedQueue<>();

    /** Snapshots reçus à appliquer sur le thread serveur. */
    private static final ConcurrentLinkedQueue<byte[]> applyQueue = new ConcurrentLinkedQueue<>();

    // ── Déclencheurs ──────────────────────────────────────────────────────────

    /** Appelé quand un pair NOUVEAU rejoint (peer_joined) — on lui envoie le snapshot. */
    public static void onNewPeer(String peerId) {
        if (sent.add(peerId)) {
            sendQueue.offer(peerId);
        }
    }

    /** Appelé pour un pair DÉJÀ présent (peer_list initiale) — on ne lui envoie rien. */
    public static void markKnown(String peerId) {
        sent.add(peerId); // marque comme "déjà traité" sans rien envoyer
    }

    public static void onPeerLeft(String peerId) {
        sent.remove(peerId);
    }

    // ── Flush (thread serveur, depuis afterChunkTick) ─────────────────────────

    public static void flush() {
        String peerId;
        while ((peerId = sendQueue.poll()) != null) {
            sendSnapshot(peerId);
        }

        byte[] data;
        while ((data = applyQueue.poll()) != null) {
            applySnapshotChunk(data);
        }
    }

    // ── Envoi ──────────────────────────────────────────────────────────────────

    private static void sendSnapshot(String peerId) {
        Object level = BlockSyncManager.levelRef.get();
        if (level == null) { sendQueue.offer(peerId); return; } // retry later

        P2PNetwork net = RuntimeInitializer.getNetwork();
        if (net == null) return;

        int cx = DistributedChunkManager.getMyChunkX();
        int cz = DistributedChunkManager.getMyChunkZ();

        System.out.println("[P2P] Snapshot → " + peerId.substring(0, 8)
            + "  centre=" + cx + "," + cz + "  rayon=" + RADIUS);

        int count = 0;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                byte[] msg = serializeChunk(level, cx + dx, cz + dz);
                if (msg != null) {
                    net.sendData(peerId, msg);
                    count++;
                }
            }
        }
        System.out.println("[P2P] Snapshot envoyé : " + count + " chunks");
    }

    private static byte[] serializeChunk(Object level, int chunkX, int chunkZ) {
        try {
            Object chunk = getChunkFromLevel(level, chunkX, chunkZ);
            if (chunk == null) return null;

            Object[] sections = getSections(chunk);
            if (sections == null || sections.length == 0) return null;

            int minY = getMinBuildHeight(level);
            int blockCount = 0;

            ByteArrayOutputStream rawBuf = new ByteArrayOutputStream(4096);
            DataOutputStream raw = new DataOutputStream(rawBuf);

            for (int si = 0; si < sections.length; si++) {
                Object sec = sections[si];
                if (sec == null || isSectionEmpty(sec)) continue;

                int baseY = minY + si * 16;

                for (int sy = 0; sy < 16; sy++) {
                    for (int sx = 0; sx < 16; sx++) {
                        for (int sz = 0; sz < 16; sz++) {
                            Object bs = getSectionBlockState(sec, sx, sy, sz);
                            if (bs == null || isAir(bs)) continue;

                            String id = BlockSyncManager.getBlockId(bs);
                            if (id == null) continue;

                            byte[] idBytes = id.getBytes(StandardCharsets.UTF_8);
                            if (idBytes.length > 127) continue;

                            raw.writeByte(sx);
                            raw.writeShort(baseY + sy);
                            raw.writeByte(sz);
                            raw.writeByte(idBytes.length);
                            raw.write(idBytes);
                            blockCount++;
                        }
                    }
                }
            }

            if (blockCount == 0) return null;

            byte[] compressed = gzip(rawBuf.toByteArray());

            // Header (non compressé) + payload compressé
            ByteBuffer msg = ByteBuffer.allocate(1 + 4 + 4 + 4 + compressed.length);
            msg.put(TYPE_SNAPSHOT);
            msg.putInt(chunkX);
            msg.putInt(chunkZ);
            msg.putInt(blockCount);
            msg.put(compressed);
            return msg.array();

        } catch (Exception e) {
            System.err.println("[P2P] Erreur sérialisation chunk " + chunkX + "," + chunkZ
                + ": " + e.getMessage());
            return null;
        }
    }

    // ── Réception ─────────────────────────────────────────────────────────────

    public static void handleReceived(byte[] data) {
        applyQueue.offer(data);
    }

    private static void applySnapshotChunk(byte[] data) {
        if (data.length < 13) return;
        try {
            ByteBuffer hdr = ByteBuffer.wrap(data, 0, 13);
            hdr.get(); // type
            int chunkX      = hdr.getInt();
            int chunkZ      = hdr.getInt();
            int blockCount  = hdr.getInt();

            byte[] rawBlocks = ungzip(Arrays.copyOfRange(data, 13, data.length));
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(rawBlocks));

            Object level = BlockSyncManager.levelRef.get();
            if (level == null) { applyQueue.offer(data); return; } // retry

            int applied = 0;
            BlockSyncManager.RECEIVING.set(true);
            try {
                for (int i = 0; i < blockCount; i++) {
                    int relX  = dis.readByte()  & 0xFF;
                    int y     = dis.readShort();
                    int relZ  = dis.readByte()  & 0xFF;
                    int idLen = dis.readByte()  & 0xFF;
                    byte[] idBytes = new byte[idLen];
                    dis.readFully(idBytes);
                    String blockId = new String(idBytes, StandardCharsets.UTF_8);

                    try {
                        Object pos   = BlockSyncManager.createBlockPos(
                                           chunkX * 16 + relX, y, chunkZ * 16 + relZ);
                        Object state = BlockSyncManager.lookupBlockState(blockId);
                        if (pos != null && state != null) {
                            // Flag 18 = 2 (send to client) | 16 (no place logic)
                            // — évite les mises à jour physiques coûteuses
                            BlockSyncManager.callSetBlock(level, pos, state, 18);
                            applied++;
                        }
                    } catch (Exception ignored) {}
                }
            } finally {
                BlockSyncManager.RECEIVING.set(false);
            }

            System.out.println("[P2P] Chunk snapshot " + chunkX + "," + chunkZ
                + " : " + applied + "/" + blockCount + " blocs");

        } catch (Exception e) {
            System.err.println("[P2P] Erreur apply snapshot: " + e.getMessage());
        }
    }

    // ── Compression ───────────────────────────────────────────────────────────

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) { gos.write(data); }
        return baos.toByteArray();
    }

    private static byte[] ungzip(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(data))) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = gis.read(buf)) != -1) baos.write(buf, 0, n);
        }
        return baos.toByteArray();
    }

    // ── Réflexion ─────────────────────────────────────────────────────────────

    private static Object getChunkFromLevel(Object level, int cx, int cz) throws Exception {
        String getChunkName = MappingsRegistry.getObfMethodName(
            "net/minecraft/world/level/Level", "getChunk");
        for (Method m : level.getClass().getMethods()) {
            if (!m.getName().equals(getChunkName) || m.getParameterCount() != 2) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p[0] == int.class && p[1] == int.class) return m.invoke(level, cx, cz);
        }
        return null;
    }

    private static Object[] getSections(Object chunk) throws Exception {
        String getSectionsName = MappingsRegistry.getObfMethodName(
            "net/minecraft/world/level/chunk/LevelChunk", "getSections");
        for (Method m : chunk.getClass().getMethods()) {
            if (m.getName().equals(getSectionsName) && m.getParameterCount() == 0) {
                Object r = m.invoke(chunk);
                if (r instanceof Object[]) return (Object[]) r;
            }
        }
        return null;
    }

    private static int getMinBuildHeight(Object level) {
        try {
            String methodName = MappingsRegistry.getObfMethodName(
                "net/minecraft/world/level/LevelHeightAccessor", "getMinBuildHeight");
            return (int) level.getClass().getMethod(methodName).invoke(level);
        } catch (Exception e) { return -64; }
    }

    private static boolean isSectionEmpty(Object sec) {
        try {
            String methodName = MappingsRegistry.getObfMethodName(
                "net/minecraft/world/level/chunk/LevelChunkSection", "hasOnlyAir");
            return (boolean) sec.getClass().getMethod(methodName).invoke(sec);
        } catch (Exception e) { return false; }
    }

    private static Object getSectionBlockState(Object sec, int x, int y, int z) {
        try {
            String methodName = MappingsRegistry.getObfMethodName(
                "net/minecraft/world/level/chunk/LevelChunkSection", "getBlockState");
            return sec.getClass()
                .getMethod(methodName, int.class, int.class, int.class)
                .invoke(sec, x, y, z);
        } catch (Exception e) { return null; }
    }

    private static boolean isAir(Object bs) {
        try {
            String methodName = MappingsRegistry.getObfMethodName(
                "net/minecraft/world/level/block/state/BlockBehaviour$BlockStateBase", "isAir");
            return (boolean) bs.getClass().getMethod(methodName).invoke(bs);
        } catch (Exception e) {
            String id = BlockSyncManager.getBlockId(bs);
            return "minecraft:air".equals(id) || "minecraft:void_air".equals(id)
                || "minecraft:cave_air".equals(id);
        }
    }
}
