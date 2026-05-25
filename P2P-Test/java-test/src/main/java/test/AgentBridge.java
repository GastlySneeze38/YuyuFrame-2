package test;

import java.util.*;

/**
 * Simule le pont Java → Rust.
 * En production : appels JNI vers la lib Rust.
 * En test : logique de distribution embarquée en Java pour valider le comportement.
 */
public class AgentBridge {

    private static final int VIEW_DISTANCE = 3;

    private final String myId;
    private int myX, myZ;
    private final Map<String, int[]> peers = new LinkedHashMap<>(); // id → [x, z]

    public AgentBridge(String myId, int x, int z) {
        this.myId = myId;
        this.myX = x;
        this.myZ = z;
    }

    public void addPeer(String id, int x, int z) {
        peers.put(id, new int[]{x, z});
    }

    public void removePeer(String id) {
        peers.remove(id);
    }

    public void updatePeerPosition(String id, int x, int z) {
        int[] pos = peers.get(id);
        if (pos != null) { pos[0] = x; pos[1] = z; }
    }

    /** Retourne true si ce peer doit calculer le tick de ce chunk */
    public boolean shouldTickChunk(int cx, int cz) {
        // Construire la liste triée des peers dans la zone
        List<String> nearby = new ArrayList<>();
        for (var e : peers.entrySet()) {
            int[] pos = e.getValue();
            if (Math.abs(pos[0] - cx) <= VIEW_DISTANCE && Math.abs(pos[1] - cz) <= VIEW_DISTANCE) {
                nearby.add(e.getKey());
            }
        }
        nearby.add(myId);
        Collections.sort(nearby); // tri stable = consensus

        long hash = Math.abs((long) cx * 73856093L + (long) cz * 19349663L);
        String owner = nearby.get((int)(hash % nearby.size()));
        return owner.equals(myId);
    }

    public int countMyChunks() {
        int count = 0;
        for (int cx = myX - VIEW_DISTANCE; cx <= myX + VIEW_DISTANCE; cx++)
            for (int cz = myZ - VIEW_DISTANCE; cz <= myZ + VIEW_DISTANCE; cz++)
                if (shouldTickChunk(cx, cz)) count++;
        return count;
    }

    public int totalChunks() {
        return (VIEW_DISTANCE * 2 + 1) * (VIEW_DISTANCE * 2 + 1);
    }
}
