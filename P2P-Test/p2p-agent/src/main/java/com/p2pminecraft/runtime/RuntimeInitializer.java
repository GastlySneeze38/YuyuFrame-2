package com.p2pminecraft.runtime;

import com.p2pminecraft.agent.AgentConfig;

/**
 * Point d'entrée du runtime, appelé par MinecraftTransformer depuis startGame().
 * Initialise le réseau P2P en lazy (une seule fois).
 */
public class RuntimeInitializer {

    private static volatile boolean initialized = false;
    private static P2PNetwork network;

    public static synchronized void onGameStart() {
        if (initialized) return;

        AgentConfig config = AgentConfig.getCurrent();
        System.out.println("[P2P] Runtime initialisation (peerId=" + config.peerId + ")");

        DistributedChunkManager.init(config.peerId, 0, 0);

        network = new P2PNetwork(config.peerId, config.peerName, config.signalingUrl);
        network.start();

        // Hook shutdown JVM
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (network != null) network.stop();
        }, "p2p-shutdown"));

        initialized = true;
        System.out.println("[P2P] Runtime prêt");
    }

    public static P2PNetwork getNetwork() { return network; }
}
