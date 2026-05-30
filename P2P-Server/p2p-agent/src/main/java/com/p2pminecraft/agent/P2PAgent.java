package com.p2pminecraft.agent;

import com.p2pminecraft.mixin.service.P2PMixinService;
import com.p2pminecraft.runtime.MappingsRegistry;
import com.p2pminecraft.runtime.RuntimeInitializer;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;

import org.objectweb.asm.*;

import java.lang.instrument.Instrumentation;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java agent P2P — utilise Mixin standalone pour les hooks bytecode.
 *
 * Ordre des javaagents dans la JVM :
 *   1. -javaagent:mixin.jar          → MixinAgent.premain() capture l'Instrumentation
 *   2. -javaagent:p2p-agent.jar=...  → ce premain charge les mappings, active Mixin + réseau P2P
 */
public class P2PAgent {

    private static final String BUILD_VERSION = "2025-05-30-v19";

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[P2P Agent] ===== VERSION " + BUILD_VERSION + " =====");
        System.out.println("[P2P Agent] Démarrage (mode Mixin)...");

        P2PMixinService.setInstrumentation(inst);

        AgentConfig config = AgentConfig.parse(agentArgs);
        System.out.println("[P2P Agent] peerId=" + config.peerId + " name=" + config.peerName);

        if (config.mappingsPath != null && !config.mappingsPath.isEmpty()) {
            try {
                MappingsRegistry.load(config.mappingsPath);
                String obfClass = MappingsRegistry.INSTANCE.map("net/minecraft/server/level/ServerLevel");
                System.out.println("[P2P] MAPPING ServerLevel → \"" + obfClass + "\""
                    + (obfClass.equals("net/minecraft/server/level/ServerLevel") ? "  ← NON MAPPÉ" : "  ← OK"));
            } catch (Exception e) {
                System.err.println("[P2P Agent] Mappings non chargés : " + e.getMessage());
            }
        } else {
            System.out.println("[P2P Agent] Aucun mappings fourni — mode JAR mappé (noms Mojang directs)");
        }

        // Découverte dynamique des classes cibles depuis les annotations @Mixin
        Set<String> mixinTargets = discoverMixinTargets();
        // gfj (Minecraft) patché via ASM direct (pas de Mixin) — doit être retransformé si déjà chargé
        mixinTargets.add("gfj");

        try {
            MixinBootstrap.init();

            if (MappingsRegistry.isLoaded()) {
                MixinEnvironment.getDefaultEnvironment().getRemappers().add(MappingsRegistry.INSTANCE);
                System.out.println("[P2P Agent] Remappeur Mojang → obfusqué enregistré dans Mixin");
            }

            try {
                java.lang.reflect.Method gotoPhase = MixinEnvironment.class
                    .getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
                gotoPhase.setAccessible(true);
                gotoPhase.invoke(null, MixinEnvironment.Phase.DEFAULT);
                System.out.println("[P2P Agent] gotoPhase(DEFAULT) OK");
            } catch (Exception ex) {
                System.err.println("[P2P Agent] gotoPhase(DEFAULT) erreur: " + ex);
            }

            Mixins.addConfiguration("mixins.p2p.json", (IMixinConfigSource) null);
            System.out.println("[P2P Agent] Config Mixin enregistrée");

            P2PMixinService.installWrapper();

            try {
                java.lang.reflect.Method injectMethod =
                    MixinBootstrap.class.getDeclaredMethod("inject");
                injectMethod.setAccessible(true);
                injectMethod.invoke(null);
                System.out.println("[P2P Agent] MixinBootstrap.inject() OK");
            } catch (Exception ex) {
                System.err.println("[P2P Agent] inject() non accessible: " + ex.getMessage());
            }

            retransformLoadedTargets(inst, mixinTargets);
        } catch (Exception e) {
            System.err.println("[P2P Agent] ERREUR Mixin bootstrap : " + e.getMessage());
            e.printStackTrace(System.err);
        }

        RuntimeInitializer.onGameStart();
        scheduleDelayedRetransform(inst, mixinTargets);

        System.out.println("[P2P Agent] Prêt — en attente du chargement Minecraft");
    }

    /**
     * Lit mixins.p2p.json puis pour chaque classe Mixin lit directement les bytes .class
     * via ASM pour extraire l'annotation @Mixin — sans passer par le class loader Java,
     * ce qui évite tout problème de classloader mismatch avec mixin.jar.
     */
    private static Set<String> discoverMixinTargets() {
        Set<String> targets = new LinkedHashSet<>();
        try {
            ClassLoader agentCL = P2PAgent.class.getClassLoader();
            try (java.io.InputStream cfgIs = agentCL.getResourceAsStream("mixins.p2p.json")) {
                if (cfgIs == null) {
                    System.err.println("[P2P Agent] mixins.p2p.json introuvable");
                    return targets;
                }
                String json = new String(cfgIs.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                String pkg  = jsonString(json, "package");
                if (pkg == null) return targets;

                int arrStart = json.indexOf('[', json.indexOf("\"mixins\""));
                int arrEnd   = json.indexOf(']', arrStart);
                if (arrStart < 0 || arrEnd < 0) return targets;

                Matcher m = Pattern.compile("\"([A-Za-z][A-Za-z0-9$]*)\"")
                        .matcher(json.substring(arrStart + 1, arrEnd));
                while (m.find()) {
                    String simpleName = m.group(1);
                    String classRes   = pkg.replace('.', '/') + "/" + simpleName + ".class";
                    System.out.println("[P2P Agent] Scan bytecode: " + simpleName);
                    try (java.io.InputStream cls = agentCL.getResourceAsStream(classRes)) {
                        if (cls == null) {
                            System.err.println("[P2P Agent]   → .class introuvable: " + classRes);
                            continue;
                        }
                        targets.addAll(extractMixinTargets(cls.readAllBytes(), simpleName));
                    } catch (Throwable e) {
                        System.err.println("[P2P Agent]   → ERREUR " + simpleName + ": " + e);
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("[P2P Agent] discoverMixinTargets erreur: " + t);
        }
        System.out.println("[P2P Agent] " + targets.size() + " cible(s) Mixin: " + targets);
        return targets;
    }

    /**
     * Utilise ASM pour lire le bytecode d'une classe Mixin et extraire les classes
     * cibles depuis l'annotation @Mixin(value/targets).
     * Aucun class loading → aucun problème de class loader mismatch.
     */
    private static Set<String> extractMixinTargets(byte[] classBytes, String simpleName) {
        Set<String> result = new LinkedHashSet<>();
        new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (!desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")) return null;
                return new AnnotationVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitArray(String name) {
                        if (name.equals("value")) {
                            return new AnnotationVisitor(Opcodes.ASM9) {
                                @Override public void visit(String n, Object val) {
                                    if (!(val instanceof Type)) return;
                                    String slash = ((Type) val).getInternalName();
                                    String obf   = MappingsRegistry.INSTANCE.map(slash);
                                    result.add(obf.replace('/', '.'));
                                    System.out.println("[P2P Agent]   → " + simpleName
                                            + " value: " + slash + " → " + obf.replace('/', '.'));
                                }
                            };
                        }
                        if (name.equals("targets")) {
                            return new AnnotationVisitor(Opcodes.ASM9) {
                                @Override public void visit(String n, Object val) {
                                    if (!(val instanceof String)) return;
                                    String slash = ((String) val).replace('.', '/');
                                    String obf   = MappingsRegistry.INSTANCE.map(slash);
                                    result.add(obf.replace('/', '.'));
                                    System.out.println("[P2P Agent]   → " + simpleName
                                            + " targets: " + val + " → " + obf.replace('/', '.'));
                                }
                            };
                        }
                        return null;
                    }
                };
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        if (result.isEmpty())
            System.err.println("[P2P Agent]   → WARN: aucune cible trouvée dans " + simpleName);
        return result;
    }

    private static String jsonString(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return null;
        i = json.indexOf('"', json.indexOf(':', i) + 1);
        if (i < 0) return null;
        int end = json.indexOf('"', i + 1);
        return end > i ? json.substring(i + 1, end) : null;
    }

    /** Retransforme les classes cibles déjà chargées au moment de l'appel. */
    private static void retransformLoadedTargets(Instrumentation inst, Set<String> targets) {
        if (targets.isEmpty()) return;
        int count = 0;
        for (Class<?> cls : inst.getAllLoadedClasses()) {
            if (!targets.contains(cls.getName())) continue;
            boolean modifiable = inst.isModifiableClass(cls);
            System.out.println("[P2P Agent] Retransform immédiat: " + cls.getName()
                    + " | modifiable=" + modifiable);
            if (!modifiable) continue;
            try {
                inst.retransformClasses(cls);
                count++;
                checkP2PHooks(cls, "après retransform immédiat");
            } catch (Throwable ex) {
                System.err.println("[P2P Agent] Retransform " + cls.getName() + " erreur: " + ex);
            }
        }
        System.out.println("[P2P Agent] Retransformations immédiates: " + count + "/" + targets.size());
    }

    /** Surveille l'apparition des classes cibles et les retransforme dès leur chargement. */
    private static void scheduleDelayedRetransform(Instrumentation inst, Set<String> targets) {
        if (targets.isEmpty()) return;
        Thread t = new Thread(() -> {
            Set<String> remaining = new LinkedHashSet<>(targets);
            long deadline = System.currentTimeMillis() + 30_000;
            while (!remaining.isEmpty() && System.currentTimeMillis() < deadline) {
                try { Thread.sleep(200); } catch (InterruptedException e) { return; }
                for (Class<?> cls : inst.getAllLoadedClasses()) {
                    if (!remaining.remove(cls.getName())) continue;
                    // Si la classe a déjà des hooks p2p$ c'est qu'elle a été mixée
                    // lors de son chargement initial (après enregistrement du transformer).
                    // retransformClasses repart des bytes originaux et ne peut pas ajouter
                    // de nouvelles méthodes → il annulerait le mixin. On saute le retransform.
                    int alreadyHooks = countP2PHooks(cls);
                    if (alreadyHooks > 0) {
                        System.out.println("[P2P Agent] Cible déjà mixée (initial load): "
                                + cls.getName() + " hooks=" + alreadyHooks + " — skip retransform");
                        checkP2PHooks(cls, "initial load (skip retransform)");
                        continue;
                    }
                    boolean mod = inst.isModifiableClass(cls);
                    System.out.println("[P2P Agent] Retransform différé: " + cls.getName()
                            + " | modifiable=" + mod);
                    if (!mod) continue;
                    try {
                        inst.retransformClasses(cls);
                        checkP2PHooks(cls, "après retransform différé");
                    } catch (Throwable ex) {
                        System.err.println("[P2P Agent] Retransform " + cls.getName() + " erreur: " + ex);
                    }
                }
            }
            if (!remaining.isEmpty()) {
                System.out.println("[P2P Agent] WARN: cibles jamais chargées: " + remaining);
            }
            System.out.println("[P2P Agent] Thread retransform terminé");
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

    private static int countP2PHooks(Class<?> cls) {
        int n = 0;
        for (java.lang.reflect.Method m : cls.getDeclaredMethods())
            if (m.getName().startsWith("p2p$")) n++;
        // Pour les classes patchées par ASM direct (ex: iqa), aucune méthode p2p$
        // n'est ajoutée. On compte quand même comme "patché" si le wrapper le signale.
        if (n == 0 && com.p2pminecraft.mixin.service.P2PMixinTransformerWrapper.isAsmPatched(cls.getName()))
            n = 1;
        return n;
    }

    private static void checkP2PHooks(Class<?> cls, String when) {
        int hooks = 0;
        for (java.lang.reflect.Method m : cls.getDeclaredMethods()) {
            if (m.getName().startsWith("p2p$")) {
                hooks++;
                System.out.println("[P2P Debug] hook: " + cls.getName() + "." + m.getName()
                        + " (" + when + ")");
            }
        }
        System.out.println("[P2P Debug] checkHooks(" + when + "): " + cls.getName()
                + " hooks=" + hooks);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        premain(agentArgs, inst);
    }
}
