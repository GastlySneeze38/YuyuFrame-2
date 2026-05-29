package com.p2pminecraft.mixin;

import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Empêche le serveur intégré de se mettre en pause quand le menu est ouvert.
 *
 * processPacketsAndTick(boolean paused) → b(Z)V dans iqa (client-mappings-1.21.11).
 * Quand paused=true, le serveur arrête de ticker (entités, redstone, etc.).
 * On intercepte l'appel, et si paused=true on le réinvoque avec paused=false.
 * Le champ p2p$inForcedTick évite la récursion infinie.
 */
@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin {

    @Shadow
    public abstract void processPacketsAndTick(boolean paused);

    @Unique
    private boolean p2p$inForcedTick = false;

    @Inject(method = "b(Z)V", at = @At("HEAD"), cancellable = true)
    private void p2p$preventPause(boolean paused, CallbackInfo ci) {
        if (paused && !this.p2p$inForcedTick) {
            this.p2p$inForcedTick = true;
            try {
                this.processPacketsAndTick(false);
            } finally {
                this.p2p$inForcedTick = false;
            }
            ci.cancel();
        }
    }
}
