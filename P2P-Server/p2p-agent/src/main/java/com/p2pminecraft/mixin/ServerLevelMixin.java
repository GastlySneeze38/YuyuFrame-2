package com.p2pminecraft.mixin;

import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    /**
     * Pas de paramètre LevelChunk dans la signature du handler pour éviter
     * tout problème de remapping de type (LevelChunk → eqq) dans le bytecode.
     * Diagnostic : confirme que tickChunk est bien intercepté.
     */
    @Inject(
        method = "tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V",
        at = @At("RETURN")
    )
    private void p2p$tickChunkReturn(CallbackInfo ci) {
        System.out.println("[P2P HOOK] tickChunk fired!");
    }
}
