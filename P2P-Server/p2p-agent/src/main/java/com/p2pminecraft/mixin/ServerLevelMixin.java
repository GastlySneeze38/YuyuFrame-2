package com.p2pminecraft.mixin;

import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    /**
     * tickTime() → d() en obfusqué, pas d'argument = pas de remapping de type.
     * Appelée à chaque tick serveur (avance le temps jour/nuit).
     * Cible confirmée dans client-mappings-1.21.11 : ServerLevel.tickTime() -> d
     *
     * ATTENTION : tickChunks() -> u appartient à ServerChunkCache, PAS ServerLevel.
     * Le mixin précédent ciblait la mauvaise classe → injection silencieusement ignorée.
     */
    // tickTime() → d()V en obfusqué dans axf (client-mappings-1.21.11 ligne ~73922)
    @Inject(method = "d()V", at = @At("HEAD"))
    private void p2p$tickTimeHead(CallbackInfo ci) {
        // Enregistrer ce ServerLevel pour que LevelChunkMixin puisse l'utiliser
        // sans @Shadow (qui nécessiterait un ClassInfo complet du JAR obfusqué).
        com.p2pminecraft.runtime.BlockSyncManager.registerLevel((Object) this);
        try {
            java.nio.file.Path f = java.nio.file.Paths.get(
                System.getenv("APPDATA"), "YuyuFrame\\p2p\\Log\\p2p_hook.txt");
            java.nio.file.Files.createDirectories(f.getParent());
            java.nio.file.Files.write(f,
                ("tickTime fired at " + System.currentTimeMillis() + "\n").getBytes(),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
    }
}
