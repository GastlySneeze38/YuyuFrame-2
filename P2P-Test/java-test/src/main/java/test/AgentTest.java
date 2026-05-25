package test;

import java.util.*;

/**
 * Simule ce que l'agent Java ferait dans Minecraft :
 * - Interception du tick de ServerLevel
 * - Délégation à AgentBridge (futur appel JNI → Rust)
 * - Vérification que la distribution fonctionne correctement
 */
public class AgentTest {

    public static void main(String[] args) {
        System.out.println("=== Test de l'agent Java (simulation sans Minecraft) ===\n");

        // --- Scénario 1 : 1 joueur seul ---
        System.out.println("[ Scénario 1 : 1 joueur seul ]");
        AgentBridge solo = new AgentBridge("player-A", 0, 0);
        System.out.printf("  Joueur A: %d/%d chunks (attendu: 100%%)%n",
                solo.countMyChunks(), solo.totalChunks());
        assert solo.countMyChunks() == solo.totalChunks() : "Solo doit avoir 100% des chunks";

        // --- Scénario 2 : 2 joueurs loin l'un de l'autre ---
        System.out.println("\n[ Scénario 2 : 2 joueurs dans des zones séparées ]");
        AgentBridge playerA = new AgentBridge("player-A", 0, 0);
        playerA.addPeer("player-B", 20, 20); // loin, zones sans overlap

        AgentBridge playerB = new AgentBridge("player-B", 20, 20);
        playerB.addPeer("player-A", 0, 0);

        System.out.printf("  Joueur A: %d/%d chunks%n", playerA.countMyChunks(), playerA.totalChunks());
        System.out.printf("  Joueur B: %d/%d chunks%n", playerB.countMyChunks(), playerB.totalChunks());
        // Zones séparées → chacun 100%
        assert playerA.countMyChunks() == playerA.totalChunks();
        assert playerB.countMyChunks() == playerB.totalChunks();

        // --- Scénario 3 : 2 joueurs dans la même zone ---
        System.out.println("\n[ Scénario 3 : 2 joueurs dans la même zone ]");
        AgentBridge p1 = new AgentBridge("player-A", 0, 0);
        p1.addPeer("player-B", 0, 0);

        AgentBridge p2 = new AgentBridge("player-B", 0, 0);
        p2.addPeer("player-A", 0, 0);

        int c1 = p1.countMyChunks();
        int c2 = p2.countMyChunks();
        System.out.printf("  Joueur A: %d/%d chunks%n", c1, p1.totalChunks());
        System.out.printf("  Joueur B: %d/%d chunks%n", c2, p2.totalChunks());
        System.out.printf("  Total: %d (attendu: %d)%n", c1 + c2, p1.totalChunks());
        assert c1 + c2 == p1.totalChunks() : "Les 2 joueurs doivent couvrir exactement 100% des chunks";

        // --- Scénario 4 : 4 joueurs dans la même zone ---
        System.out.println("\n[ Scénario 4 : 4 joueurs dans la même zone ]");
        String[] ids = {"player-A", "player-B", "player-C", "player-D"};
        AgentBridge[] players = new AgentBridge[4];
        for (int i = 0; i < 4; i++) {
            players[i] = new AgentBridge(ids[i], 0, 0);
            for (int j = 0; j < 4; j++) {
                if (j != i) players[i].addPeer(ids[j], 0, 0);
            }
        }

        int total4 = 0;
        for (int i = 0; i < 4; i++) {
            int cnt = players[i].countMyChunks();
            total4 += cnt;
            System.out.printf("  %s: %d/%d chunks (%d%%)%n",
                    ids[i], cnt, players[i].totalChunks(),
                    cnt * 100 / players[i].totalChunks());
        }
        System.out.printf("  Total: %d (attendu: %d)%n", total4, players[0].totalChunks());
        assert total4 == players[0].totalChunks() : "4 joueurs doivent couvrir exactement 100%";

        // --- Simulation du tick ---
        System.out.println("\n[ Simulation du tick ServerLevel ]");
        AgentBridge bridge = new AgentBridge("player-A", 0, 0);
        bridge.addPeer("player-B", 1, 0);

        List<FakeChunk> world = new ArrayList<>();
        for (int cx = -5; cx <= 5; cx++)
            for (int cz = -5; cz <= 5; cz++)
                world.add(new FakeChunk(cx, cz));

        int ticked = 0, skipped = 0;
        for (FakeChunk chunk : world) {
            if (bridge.shouldTickChunk(chunk.x, chunk.z)) {
                chunk.tick(); // <-- c'est ici que l'agent Java appelle via JNI
                ticked++;
            } else {
                skipped++;
            }
        }
        System.out.printf("  Ticked: %d | Skipped (autre peer): %d | Total: %d%n",
                ticked, skipped, world.size());

        System.out.println("\n=== Tous les tests passent ===");
    }
}
