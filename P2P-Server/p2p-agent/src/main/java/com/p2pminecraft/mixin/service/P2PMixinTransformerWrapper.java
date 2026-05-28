package com.p2pminecraft.mixin.service;

import com.p2pminecraft.runtime.MappingsRegistry;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * Adapte IMixinTransformer → ClassFileTransformer.
 *
 * transformClassBytes(name, transformedName, bytes) :
 *   name            = nom obfusqué (ex: "axf")
 *   transformedName = nom Mojang déobfusqué (ex: "net.minecraft.server.level.ServerLevel")
 * Mixin utilise transformedName pour matcher les cibles @Mixin.
 */
public class P2PMixinTransformerWrapper implements ClassFileTransformer {

    private final IMixinTransformer transformer;

    public P2PMixinTransformerWrapper(IMixinTransformer transformer) {
        this.transformer = transformer;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain domain,
                            byte[] classfileBuffer) {
        if (classfileBuffer == null || className == null) return null;

        // name = obfusqué (slashes → dots), transformedName = Mojang déobfusqué
        String obfDot = className.replace('/', '.');
        String mojangSlash = MappingsRegistry.INSTANCE.unmap(className);
        String mojangDot = (mojangSlash != null && !mojangSlash.equals(className))
                ? mojangSlash.replace('/', '.')
                : obfDot;

        boolean isTarget = className.equals("eqq") || className.equals("axf")
                || className.contains("LevelChunk") || className.contains("ServerLevel");
        if (isTarget) {
            System.out.println("[P2P] transform: " + className + " (mojang=" + mojangDot + ")");
        }

        try {
            byte[] result = transformer.transformClassBytes(obfDot, mojangDot, classfileBuffer);
            if (isTarget) {
                if (result == null) {
                    System.out.println("[P2P] transformClassBytes → null (pas de changement) pour " + className);
                } else if (result == classfileBuffer) {
                    System.out.println("[P2P] transformClassBytes → mêmes bytes pour " + className);
                } else {
                    System.out.println("[P2P] MIXIN APPLIQUÉ sur " + className + " (" + result.length + " bytes)");
                }
            }
            return result;
        } catch (Throwable t) {
            System.err.println("[P2P] Erreur transform " + obfDot + ": " + t);
            t.printStackTrace(System.err);
            return classfileBuffer;
        }
    }
}
