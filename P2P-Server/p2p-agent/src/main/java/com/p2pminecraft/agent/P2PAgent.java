package com.p2pminecraft.agent;

import com.p2pminecraft.runtime.MappingsRegistry;
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
 *   2. -javaagent:p2p-agent.jar=...  → ce premain charge les mappings, active Mixin + réseau P2P
 *
 * Le paramètre optionnel mappings= pointe vers le fichier ProGuard Mojang.
 * Quand présent, MappingsRegistry est initialisé et ajouté à la chaîne de remapping Mixin
 * pour que LevelChunkMixin cible correctement le JAR Minecraft obfusqué non mappé.
 */
public class P2PAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[P2P Agent] Démarrage (mode Mixin)...");

        AgentConfig config = AgentConfig.parse(agentArgs);
        System.out.println("[P2P Agent] peerId=" + config.peerId + " name=" + config.peerName);

        // Chargement des mappings Mojang (si fournis)
        if (config.mappingsPath != null && !config.mappingsPath.isEmpty()) {
            try {
                MappingsRegistry.load(config.mappingsPath);
            } catch (Exception e) {
                System.err.println("[P2P Agent] Mappings non chargés (" + config.mappingsPath + ") : " + e.getMessage());
            }
        } else {
            System.out.println("[P2P Agent] Aucun mappings fourni — mode JAR mappé (noms Mojang directs)");
        }

        // Active Mixin : MixinAgent (mixin.jar) a déjà capturé l'Instrumentation.
        // Le remappeur MappingsRegistry est enregistré AVANT addConfiguration pour que
        // la résolution des classes cibles (@Mixin) utilise les noms obfusqués corrects.
        try {
            MixinBootstrap.init();
            MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);

            if (MappingsRegistry.isLoaded()) {
                MixinEnvironment.getDefaultEnvironment().getRemappers().add(MappingsRegistry.INSTANCE);
                System.out.println("[P2P Agent] Remappeur Mojang → obfusqué enregistré dans Mixin");
            }

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
