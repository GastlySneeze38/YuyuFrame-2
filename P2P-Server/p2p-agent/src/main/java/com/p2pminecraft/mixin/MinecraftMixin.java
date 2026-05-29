package com.p2pminecraft.mixin;

import com.p2pminecraft.runtime.RuntimeInitializer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Empêche la mise en pause du monde quand P2P est actif.
 *
 * Utilise @Mixin(Minecraft.class) via stub (pas targets="...") pour que la résolution
 * de la classe cible se fasse au moment du TRANSFORM (mappings chargés), et non au
 * PARSING de la config (mappings pas encore disponibles).
 *
 * Deux hooks dans la classe Minecraft (gfj, client-mappings-1.21.11) :
 *   - isPaused()       → an()Z : getter lu par IntegratedServer pour stopper le tick
 *   - isSingleplayer() → ab()Z : check upstream qui autorise le setter de pause
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "an()Z", at = @At("HEAD"), cancellable = true)
    private void p2p$preventPause(CallbackInfoReturnable<Boolean> cir) {
        if (RuntimeInitializer.getNetwork() != null) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "ab()Z", at = @At("HEAD"), cancellable = true)
    private void p2p$fakeMultiplayer(CallbackInfoReturnable<Boolean> cir) {
        if (RuntimeInitializer.getNetwork() != null) {
            cir.setReturnValue(false);
        }
    }
}
