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
        // Test de capture STDOUT : si cette ligne apparaît dans les logs Minecraft
        // sous [STDOUT], System.out de l'agent est bien redirigé.
        System.out.println("[P2P] === STDOUT_DIAGNOSTIC === agent démarré, System.out opérationnel ===");
        System.out.println("[P2P Agent] Démarrage (mode Mixin)...");

        // Sauvegarder l'Instrumentation pour que offer() puisse l'utiliser
        P2PMixinService.setInstrumentation(inst);

        AgentConfig config = AgentConfig.parse(agentArgs);
        System.out.println("[P2P Agent] peerId=" + config.peerId + " name=" + config.peerName);

        // Chargement des mappings Mojang (si fournis)
        if (config.mappingsPath != null && !config.mappingsPath.isEmpty()) {
            try {
                MappingsRegistry.load(config.mappingsPath);
                // Diagnostic critique : vérifier que tickChunks est mappé
                // Si le résultat == "tickChunks", le mapping ne contient pas cette méthode
                // → Mixin cherchera "tickChunks" dans axf → introuvable → injection ratée
                String obfTick = MappingsRegistry.getObfMethodName(
                    "net/minecraft/server/level/ServerLevel", "tickChunks");
                System.out.println("[P2P] MAPPING tickChunks → \"" + obfTick + "\""
                    + (obfTick.equals("tickChunks")
                       ? "  ← NON MAPPÉ : méthode absente des client-mappings, hook impossible!"
                       : "  ← OK"));
                // Même diagnostic pour la classe cible
                String obfClass = com.p2pminecraft.runtime.MappingsRegistry.INSTANCE
                    .map("net/minecraft/server/level/ServerLevel");
                System.out.println("[P2P] MAPPING ServerLevel → \"" + obfClass + "\""
                    + (obfClass.equals("net/minecraft/server/level/ServerLevel")
                       ? "  ← NON MAPPÉ"
                       : "  ← OK"));
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

            if (MappingsRegistry.isLoaded()) {
                MixinEnvironment.getDefaultEnvironment().getRemappers().add(MappingsRegistry.INSTANCE);
                System.out.println("[P2P Agent] Remappeur Mojang → obfusqué enregistré dans Mixin");
            }

            // mixin.jar s'initialise en phase PREINIT (son propre getInitialPhase()).
            // Les mixins dans le tableau "mixins" du JSON sont pour la phase DEFAULT.
            // Si on addConfiguration() en PREINIT, selectConfigs(PREINIT) ne trouve
            // pas ces mixins → jamais préparés → transformClassBytes retourne null.
            // Fix : avancer la phase vers DEFAULT AVANT addConfiguration() pour que
            // la config soit associée au bon environnement.
            try {
                java.lang.reflect.Method gotoPhase = MixinEnvironment.class
                    .getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
                gotoPhase.setAccessible(true);
                gotoPhase.invoke(null, MixinEnvironment.Phase.DEFAULT);
                System.out.println("[P2P Agent] gotoPhase(DEFAULT) OK");
                writeDebug("gotoPhase(DEFAULT) OK\n");
            } catch (Exception ex) {
                System.err.println("[P2P Agent] gotoPhase(DEFAULT) erreur: " + ex);
                writeDebug("gotoPhase(DEFAULT) ERREUR: " + ex + "\n");
            }

            Mixins.addConfiguration("mixins.p2p.json", (IMixinConfigSource) null);
            System.out.println("[P2P Agent] Config Mixin enregistrée (phase DEFAULT)");

            P2PMixinService.installWrapper();

            // inject() déclenche select(DEFAULT) + redefineClasses pour les classes
            // déjà chargées — en phase DEFAULT, nos mixins sont éligibles.
            try {
                java.lang.reflect.Method injectMethod =
                    MixinBootstrap.class.getDeclaredMethod("inject");
                injectMethod.setAccessible(true);
                injectMethod.invoke(null);
                System.out.println("[P2P Agent] MixinBootstrap.inject() OK");
            } catch (Exception ex) {
                System.err.println("[P2P Agent] inject() non accessible: " + ex.getMessage());
            }

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
            boolean foundAxf = false, foundEqq = false;
            for (Class<?> cls : inst.getAllLoadedClasses()) {
                String name = cls.getName();
                if (name.equals("eqq") || name.equals("axf")) {
                    boolean modifiable = inst.isModifiableClass(cls);
                    System.out.println("[P2P Agent] Retransform immédiat: " + name
                            + " | modifiable=" + modifiable);
                    writeDebug("retransform: " + name + " modifiable=" + modifiable + "\n");
                    if (name.equals("axf")) checkP2PHooks(cls, "avant retransform immédiat");
                    if (modifiable) {
                        try {
                            inst.retransformClasses(cls);
                            writeDebug("retransformClasses(" + name + ") OK\n");
                            count++;
                        } catch (Throwable ex) {
                            writeDebug("retransformClasses(" + name + ") ERREUR: " + ex + "\n");
                            System.err.println("[P2P Agent] Retransform " + name + " erreur: " + ex);
                        }
                    }
                    if (name.equals("axf")) {
                        checkP2PHooks(cls, "après retransform immédiat");
                        foundAxf = true;
                    }
                    if (name.equals("eqq")) foundEqq = true;
                }
            }
            System.out.println("[P2P Agent] Retransformations immédiates: " + count
                    + " (axf=" + foundAxf + " eqq=" + foundEqq + ")");
            if (!foundAxf) {
                System.out.println("[P2P Agent] WARN: axf (ServerLevel) pas encore chargé — sera traité par le thread différé");
            }
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
                        boolean mod = inst.isModifiableClass(cls);
                        System.out.println("[P2P Agent] Retransformation différée: eqq | modifiable=" + mod);
                        try { if (mod) inst.retransformClasses(cls); }
                        catch (Throwable ex) { System.err.println("[P2P Agent] Retransform eqq erreur: " + ex); }
                    }
                    if (!foundAxf && name.equals("axf")) {
                        foundAxf = true;
                        boolean mod = inst.isModifiableClass(cls);
                        // Vérifier si MixinAgent a déjà injecté le hook (avant notre retransform)
                        checkP2PHooks(cls, "avant retransform différé");
                        System.out.println("[P2P Agent] Retransformation différée: axf | modifiable=" + mod);
                        try { if (mod) inst.retransformClasses(cls); }
                        catch (Throwable ex) { System.err.println("[P2P Agent] Retransform axf erreur: " + ex); }
                        // Vérifier après (via reflection — peut ne pas refléter les nouvelles méthodes immédiatement)
                        checkP2PHooks(cls, "après retransform différé");
                    }
                }
            }
            System.out.println("[P2P Agent] Thread retransform terminé (eqq=" + foundEqq + " axf=" + foundAxf + ")");
        }, "P2P-Retransform");
        t.setDaemon(true);
        t.start();
    }

    private static void writeDebug(String msg) {
        try {
            java.nio.file.Path f = java.nio.file.Paths.get(
                System.getenv("APPDATA"), "YuyuFrame\\p2p\\Log\\p2p_agent.txt");
            java.nio.file.Files.createDirectories(f.getParent());
            java.nio.file.Files.write(f, msg.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
    }

    private static void checkP2PHooks(Class<?> cls, String when) {
        try {
            int total = 0;
            int hooks = 0;
            for (java.lang.reflect.Method m : cls.getDeclaredMethods()) {
                total++;
                if (m.getName().startsWith("p2p$")) {
                    hooks++;
                    String msg = "hook: " + cls.getName() + "." + m.getName() + " (" + when + ")\n";
                    System.out.println("[P2P Debug] " + msg.trim());
                    writeDebug(msg);
                }
            }
            String summary = "checkHooks(" + when + "): " + cls.getName()
                + " hooks=" + hooks + "/" + total + "\n";
            System.out.println("[P2P Debug] " + summary.trim());
            writeDebug(summary);
        } catch (Throwable t) {
            writeDebug("checkP2PHooks error: " + t + "\n");
            System.err.println("[P2P Debug] checkP2PHooks error: " + t);
        }
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        premain(agentArgs, inst);
    }
}
