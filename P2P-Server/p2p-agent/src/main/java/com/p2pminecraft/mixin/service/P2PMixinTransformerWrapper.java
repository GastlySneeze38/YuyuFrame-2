package com.p2pminecraft.mixin.service;

import com.p2pminecraft.runtime.MappingsRegistry;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * Adapte IMixinTransformer → ClassFileTransformer.
 *
 * transformClassBytes(name, transformedName, bytes) :
 *   name            = nom obfusqué pointillé (ex: "axf")
 *   transformedName = nom Mojang pointillé   (ex: "net.minecraft.server.level.ServerLevel")
 * Mixin utilise transformedName pour matcher les cibles @Mixin.
 * MappingsRegistry.unmap() retrouve le nom Mojang depuis le nom obfusqué.
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

        // Mixin stocke les cibles par nom Mojang (dot-format).
        // transformClassBytes(name, transformedName, bytes) :
        //   name            = nom binaire obfusqué (ex: "axf")
        //   transformedName = nom Mojang pointillé (ex: "net.minecraft.server.level.ServerLevel")
        // Mixin utilise transformedName pour le lookup du registre de cibles.
        // On doit donc toujours passer le nom Mojang comme transformedName.
        String obfDot    = className.replace('/', '.');
        String mojangDot = MappingsRegistry.INSTANCE.unmap(className).replace('/', '.');

        return transformer.transformClassBytes(obfDot, mojangDot, classfileBuffer);
    }
}
