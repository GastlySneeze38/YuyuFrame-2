package com.p2pminecraft.mixin.service;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.*;
import org.spongepowered.asm.util.ReEntranceLock;

import java.lang.instrument.Instrumentation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

/**
 * Service Mixin standalone pour un Java agent sans LaunchWrapper/ModLauncher.
 * Enregistré via META-INF/services, sélectionné car isValid() = true.
 */
public class P2PMixinService implements IMixinService, IClassProvider, IClassBytecodeProvider {

    private static volatile Instrumentation savedInst;
    // Transformer créé dans offer() — conservé pour installation différée si savedInst était null
    private static volatile IMixinTransformer storedTransformer;

    public static void setInstrumentation(Instrumentation inst) {
        savedInst = inst;
        // Pas d'installation automatique ici : les mappings ne sont pas encore chargés.
        // P2PAgent.premain() appellera installWrapper() explicitement après setup complet.
    }

    private final ReEntranceLock lock = new ReEntranceLock(1);
    private final IContainerHandle container =
        new P2PContainerHandle("p2p-agent", "YuyuFrame P2P Agent");

    // ── IMixinService ─────────────────────────────────────────────────────────

    @Override public String getName()  { return "P2PJavaAgent"; }
    @Override public boolean isValid() { return true; }
    @Override public void prepare()    {}
    @Override public void init()       {}
    @Override public void beginPhase() {}
    @Override
    public void offer(IMixinInternal internal) {
        if (!(internal instanceof IMixinTransformerFactory)) return;
        try {
            storedTransformer = ((IMixinTransformerFactory) internal).createTransformer();
            // Pas d'installation ici : mappings pas encore chargés → unmap() retournerait le
            // nom obfusqué tel quel au lieu du nom Mojang → Mixin ne reconnaîtrait pas la cible.
            System.out.println("[P2P] offer() : transformer stocké, wrapper installé plus tard");
        } catch (Exception e) {
            System.err.println("[P2P] Erreur offer(): " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    /** Appelé explicitement par P2PAgent.premain() APRÈS chargement mappings + addConfiguration(). */
    public static void installWrapper() {
        if (savedInst == null) { System.err.println("[P2P] installWrapper: savedInst null"); return; }
        if (storedTransformer == null) { System.err.println("[P2P] installWrapper: storedTransformer null"); return; }
        try {
            savedInst.addTransformer(new P2PMixinTransformerWrapper(storedTransformer), true);
            System.out.println("[P2P] Wrapper installé (mappings+config prêts, canRetransform=true)");
        } catch (Exception e) {
            System.err.println("[P2P] installWrapper() erreur: " + e);
        }
    }
    @Override public void checkEnv(Object bootSource)    {}

    @Override
    public MixinEnvironment.Phase getInitialPhase() {
        // DEFAULT : nos mixins sont dans le tableau "mixins" de mixins.p2p.json (phase DEFAULT).
        // Avec PREINIT, ils étaient préparés (parsés) mais jamais appliqués — la phase DEFAULT
        // n'arrivait jamais dans notre setup standalone sans launcher.
        return MixinEnvironment.Phase.DEFAULT;
    }

    @Override public ReEntranceLock getReEntranceLock()     { return lock; }
    @Override public IClassProvider getClassProvider()      { return this; }
    @Override public IClassBytecodeProvider getBytecodeProvider() { return this; }
    @Override public ITransformerProvider getTransformerProvider() { return null; }
    @Override public IClassTracker getClassTracker()        { return null; }
    @Override public IMixinAuditTrail getAuditTrail()       { return null; }

    @Override
    public Collection<String> getPlatformAgents() { return Collections.emptyList(); }

    @Override
    public IContainerHandle getPrimaryContainer() { return container; }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        return Collections.emptyList();
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        // Essayer d'abord le context classloader (MC), puis l'agent classloader pour les
        // ressources embarquées dans notre JAR (mixins.p2p.json, services SPI, etc.)
        ClassLoader cl = getContextClassLoader();
        InputStream is = cl.getResourceAsStream(name);
        if (is == null) {
            cl = P2PMixinService.class.getClassLoader();
            is = cl.getResourceAsStream(name);
        }
        writeNodeDebug("getResourceAsStream: " + name + " found=" + (is != null) + "\n");
        return is;
    }

    @Override public String getSideName() { return "CLIENT"; }

    @Override
    public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() { return null; }

    @Override
    public MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel() { return null; }

    @Override
    public ILogger getLogger(String name) { return new P2PLogger(name); }

    // ── IClassProvider ────────────────────────────────────────────────────────

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return Class.forName(name, false, getContextClassLoader());
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, getContextClassLoader());
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, P2PMixinService.class.getClassLoader());
    }

    @Override
    public URL[] getClassPath() { return new URL[0]; }

    // ── IClassBytecodeProvider ────────────────────────────────────────────────

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        return getClassNode(name, false, 0);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers)
            throws ClassNotFoundException, IOException {
        return getClassNode(name, runTransformers, 0);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags)
            throws ClassNotFoundException, IOException {
        // Mixin passe le nom en format "a.b.C" ou "a/b/C" selon le contexte
        String nameSlash = name.replace('.', '/');
        String resource = nameSlash + ".class";

        // 1. Chercher directement (nos propres classes, classes JDK, classes déjà non-obf)
        ClassLoader cl = getContextClassLoader();
        InputStream is = cl.getResourceAsStream(resource);
        if (is == null) {
            cl = P2PMixinService.class.getClassLoader();
            is = cl.getResourceAsStream(resource);
        }

        // 2. Pas trouvé → c'est probablement un nom Mojang (net/minecraft/...).
        //    Convertir via MappingsRegistry vers le nom obfusqué pour charger depuis le JAR MC.
        String obfSlash = nameSlash;
        if (is == null && com.p2pminecraft.runtime.MappingsRegistry.isLoaded()) {
            obfSlash = com.p2pminecraft.runtime.MappingsRegistry.INSTANCE.map(nameSlash);
            if (!obfSlash.equals(nameSlash)) {
                String obfResource = obfSlash + ".class";
                cl = getContextClassLoader();
                is = cl.getResourceAsStream(obfResource);
                if (is == null) {
                    cl = P2PMixinService.class.getClassLoader();
                    is = cl.getResourceAsStream(obfResource);
                }
            }
        }

        writeNodeDebug("getClassNode: " + nameSlash + " found=" + (is != null)
                + (obfSlash.equals(nameSlash) ? "" : " (obf=" + obfSlash + ")") + "\n");
        if (is == null) throw new ClassNotFoundException(name);

        try {
            ClassReader cr = new ClassReader(is);
            ClassNode cn = new ClassNode();
            cr.accept(cn, readerFlags == 0 ? ClassReader.EXPAND_FRAMES : readerFlags);
            // Si on a chargé la classe obfusquée pour répondre à une demande de nom Mojang,
            // corriger cn.name pour qu'il corresponde à ce que Mixin attend.
            if (!obfSlash.equals(nameSlash)) {
                cn.name = nameSlash;
            }
            return cn;
        } finally {
            is.close();
        }
    }

    private static void writeNodeDebug(String msg) {
        try {
            java.nio.file.Path f = java.nio.file.Paths.get(
                System.getenv("APPDATA"), "YuyuFrame\\p2p\\Log\\p2p_agent.txt");
            java.nio.file.Files.createDirectories(f.getParent());
            java.nio.file.Files.write(f, msg.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ClassLoader getContextClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl != null ? cl : ClassLoader.getSystemClassLoader();
    }
}
