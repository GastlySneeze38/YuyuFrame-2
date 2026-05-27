package com.p2pminecraft.agent;

import com.p2pminecraft.runtime.RuntimeInitializer;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.lang.instrument.Instrumentation;

/**
 * Java agent P2P — utilise Mixin standalone pour les hooks bytecode.
 *
 * Ordre des javaagents dans la JVM :
 *   1. -javaagent:mixin.jar          → MixinAgent.premain() capture l'Instrumentation
 *   2. -javaagent:p2p-agent.jar=...  → ce premain active Mixin + réseau P2P
 */
public class P2PAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[P2P Agent] Démarrage (mode Mixin)...");

        AgentConfig config = AgentConfig.parse(agentArgs);
        System.out.println("[P2P Agent] peerId=" + config.peerId + " name=" + config.peerName);

        // Active Mixin : MixinAgent (mixin.jar) a déjà capturé l'Instrumentation.
        // MixinBootstrap découvre notre P2PMixinService via ServiceLoader.
        try {
            MixinBootstrap.init();
            MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
            Mixins.addConfiguration("mixins.p2p.json");
            System.out.println("[P2P Agent] Mixin initialisé — LevelChunkMixin enregistré");
        } catch (Exception e) {
            System.err.println("[P2P Agent] ERREUR Mixin bootstrap : " + e.getMessage());
            e.printStackTrace(System.err);
        }

        // Démarre le réseau P2P (P2PNetwork, WebSocket signaling)
        RuntimeInitializer.onGameStart();

        System.out.println("[P2P Agent] Prêt — en attente du chargement Minecraft");
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        premain(agentArgs, inst);
    }
}
