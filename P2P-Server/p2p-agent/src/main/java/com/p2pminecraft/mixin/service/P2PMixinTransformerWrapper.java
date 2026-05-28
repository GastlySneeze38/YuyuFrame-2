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

        // className utilise des slashes ("axf", "net/minecraft/...")
        // transformClassBytes attend des points ("axf", "net.minecraft...")
        String obfDot    = className.replace('/', '.');
        // unmap() retrouve le nom Mojang (slashes) depuis le nom obfusqué (slashes) — pour le log seulement
        String mojangDot = MappingsRegistry.INSTANCE.unmap(className).replace('/', '.');

        boolean isTarget = "axf".equals(className) || "eqq".equals(className)
                || className.contains("ServerLevel") || className.contains("LevelChunk");

        if (isTarget) {
            String msg = "transform: " + className + " → " + mojangDot
                    + " | retransform=" + (classBeingRedefined != null) + "\n";
            System.out.println("[P2P Wrapper] " + msg.trim());
            try {
                java.nio.file.Path f = java.nio.file.Paths.get(
                    System.getenv("APPDATA"), "YuyuFrame\\p2p\\Log\\p2p_wrapper.txt");
                java.nio.file.Files.createDirectories(f.getParent());
                java.nio.file.Files.write(f, msg.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception ignored) {}
        }

        if (isTarget) {
            // Diagnostic Mixin : combien de configs sont "non visitées" (pending) ?
            try {
                int unvisited = org.spongepowered.asm.mixin.Mixins.getUnvisitedCount();
                java.util.Collection<?> cfgs = org.spongepowered.asm.mixin.Mixins.getConfigs();
                String state = "mixinState[" + className + "]: unvisited=" + unvisited
                        + " totalConfigs=" + cfgs.size() + "\n";
                System.out.println("[P2P Wrapper] " + state.trim());
                java.nio.file.Path f = java.nio.file.Paths.get(
                    System.getenv("APPDATA"), "YuyuFrame\\p2p\\Log\\p2p_wrapper.txt");
                java.nio.file.Files.write(f, state.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception ignored) {}
        }

        // Essai 1 : obfDot/obfDot (Mixin stocke la cible remappée "axf")
        // Essai 2 : obfDot/mojangDot (au cas où Mixin stockerait le nom Mojang)
        byte[] result = transformer.transformClassBytes(obfDot, obfDot, classfileBuffer);
        if (isTarget && (result == null || result == classfileBuffer)) {
            // Essai 2 : autre forme du nom
            byte[] result2 = transformer.transformClassBytes(obfDot, mojangDot, classfileBuffer);
            if (result2 != null && result2 != classfileBuffer) {
                result = result2;
            }
        }

        if (isTarget) {
            boolean modified = result != null && result != classfileBuffer;
            String msg2 = "result: modified=" + modified + " for " + className + "\n";
            try {
                java.nio.file.Path f = java.nio.file.Paths.get(
                    System.getenv("APPDATA"), "YuyuFrame\\p2p\\Log\\p2p_wrapper.txt");
                java.nio.file.Files.write(f, msg2.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception ignored) {}
        }

        return result;
    }
}
