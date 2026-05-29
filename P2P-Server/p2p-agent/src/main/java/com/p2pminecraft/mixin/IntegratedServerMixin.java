package com.p2pminecraft.mixin;

import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Empêche le serveur intégré de se mettre en pause quand le menu est ouvert.
 *
 * processPacketsAndTick(boolean paused) → b(Z)V dans iqa (client-mappings-1.21.11).
 * Intercepte paused=true, appelle b(false) via réflexion pour forcer un tick complet,
 * puis annule l'appel original.
 * L'appel réflexif avec paused=false passe le guard "if (!paused) return;" sans récursion.
 *
 * Pas de @Shadow : évite les problèmes de remapping lors du merge Mixin.
 */
@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin {

    @Inject(method = "b(Z)V", at = @At("HEAD"), cancellable = true)
    private void p2p$preventPause(boolean paused, CallbackInfo ci) {
        if (!paused) return;
        System.out.println("[P2P] p2p$preventPause — forcing tick (paused=true intercepted)");
        try {
            for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                if ("b".equals(m.getName())
                        && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == boolean.class) {
                    m.setAccessible(true);
                    m.invoke(this, Boolean.FALSE);
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("[P2P] p2p$preventPause error: " + e);
        }
        ci.cancel();
    }
}
