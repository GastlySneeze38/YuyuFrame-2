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
import org.spongepowered.tools.agent.MixinAgent;

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
        System.out.println("[P2P] offer() reçu : " + internal.getClass().getName());
        for (Class<?> iface : internal.getClass().getInterfaces()) {
            System.out.println("[P2P]   iface: " + iface.getName() + " (CL: " + iface.getClassLoader() + ")");
        }
        System.out.println("[P2P]   IMixinTransformerFactory CL: " + IMixinTransformerFactory.class.getClassLoader());
        System.out.println("[P2P]   instanceof check: " + (internal instanceof IMixinTransformerFactory));

        if (internal instanceof IMixinTransformerFactory) {
            try {
                IMixinTransformer transformer = ((IMixinTransformerFactory) internal).createTransformer();
                new MixinAgent(transformer);
                System.out.println("[P2P] MixinAgent installé — ClassFileTransformer actif");
            } catch (Exception e) {
                System.err.println("[P2P] Erreur installation MixinAgent: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
    }
    @Override public void checkEnv(Object bootSource)    {}

    @Override
    public MixinEnvironment.Phase getInitialPhase() {
        return MixinEnvironment.Phase.PREINIT;
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
        ClassLoader cl = getContextClassLoader();
        return cl.getResourceAsStream(name);
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
        String resource = name.replace('.', '/') + ".class";
        try (InputStream is = getContextClassLoader().getResourceAsStream(resource)) {
            if (is == null) throw new ClassNotFoundException(name);
            ClassReader cr = new ClassReader(is);
            ClassNode cn = new ClassNode();
            cr.accept(cn, readerFlags == 0 ? ClassReader.EXPAND_FRAMES : readerFlags);
            return cn;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ClassLoader getContextClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl != null ? cl : ClassLoader.getSystemClassLoader();
    }
}
