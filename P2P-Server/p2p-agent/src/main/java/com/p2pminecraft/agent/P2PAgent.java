package com.p2pminecraft.agent;

import com.p2pminecraft.mixin.service.P2PMixinService;
import com.p2pminecraft.runtime.MappingsRegistry;
import com.p2pminecraft.runtime.RuntimeInitializer;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;

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

        // Sauvegarder l'Instrumentation pour que offer() puisse l'utiliser
        P2PMixinService.setInstrumentation(inst);

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

            // addConfiguration(String,IMixinConfigSource) passe getDefaultEnvironment() (non-null)
            // contrairement à addConfiguration(String) qui passe null → NPE dans MixinConfig.onLoad()
            Mixins.addConfiguration("mixins.p2p.json", (IMixinConfigSource) null);
            System.out.println("[P2P Agent] Mixin initialisé — LevelChunkMixin enregistré");

            // eqq (LevelChunk) et axf (ServerLevel) sont chargés AVANT addTransformer
            // → on les retransforme maintenant que la config est enregistrée
            retransformEarlyLoadedTargets(inst);
        } catch (Exception e) {
            System.err.println("[P2P Agent] ERREUR Mixin bootstrap : " + e.getMessage());
            e.printStackTrace(System.err);
        }

        // Démarre le réseau P2P (P2PNetwork, WebSocket signaling)
        RuntimeInitializer.onGameStart();

        // Retransformation différée : certaines classes peuvent être chargées plus tard
        scheduleDelayedRetransform(inst);

        System.out.println("[P2P Agent] Prêt — en attente du chargement Minecraft");
    }

    /** Retransforme eqq/axf s'ils ont été chargés avant notre addTransformer. */
    private static void retransformEarlyLoadedTargets(Instrumentation inst) {
        try {
            int count = 0;
            for (Class<?> cls : inst.getAllLoadedClasses()) {
                String name = cls.getName();
                if (name.equals("eqq") || name.equals("axf")) {
                    System.out.println("[P2P Agent] Retransformation immédiate: " + name);
                    if (inst.isModifiableClass(cls)) {
                        inst.retransformClasses(cls);
                        count++;
                    }
                }
            }
            System.out.println("[P2P Agent] Retransformations immédiates: " + count);
        } catch (Throwable t) {
            System.err.println("[P2P Agent] Retransform immédiat erreur: " + t);
        }
    }

    /** Lance un thread qui retransforme eqq/axf dès qu'ils apparaissent dans les classes chargées. */
    private static void scheduleDelayedRetransform(Instrumentation inst) {
        Thread t = new Thread(() -> {
            long deadline = System.currentTimeMillis() + 30_000;
            boolean foundEqq = false, foundAxf = false;
            while (System.currentTimeMillis() < deadline && !(foundEqq && foundAxf)) {
                try { Thread.sleep(200); } catch (InterruptedException e) { return; }
                for (Class<?> cls : inst.getAllLoadedClasses()) {
                    String name = cls.getName();
                    if (!foundEqq && name.equals("eqq")) {
                        foundEqq = true;
                        System.out.println("[P2P Agent] Retransformation différée: eqq");
                        try { if (inst.isModifiableClass(cls)) inst.retransformClasses(cls); }
                        catch (Throwable ex) { System.err.println("[P2P Agent] Retransform eqq erreur: " + ex); }
                    }
                    if (!foundAxf && name.equals("axf")) {
                        foundAxf = true;
                        System.out.println("[P2P Agent] Retransformation différée: axf");
                        try { if (inst.isModifiableClass(cls)) inst.retransformClasses(cls); }
                        catch (Throwable ex) { System.err.println("[P2P Agent] Retransform axf erreur: " + ex); }
                    }
                }
            }
            System.out.println("[P2P Agent] Thread retransform terminé (eqq=" + foundEqq + " axf=" + foundAxf + ")");
        }, "P2P-Retransform");
        t.setDaemon(true);
        t.start();
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        premain(agentArgs, inst);
    }
}
