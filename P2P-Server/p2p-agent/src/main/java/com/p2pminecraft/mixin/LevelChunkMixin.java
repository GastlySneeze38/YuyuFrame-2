package com.p2pminecraft.mixin;

import com.p2pminecraft.runtime.BlockSyncManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

    @Shadow public Level level;

    /**
     * Détecte chaque changement de bloc local et le diffuse aux pairs.
     * setBlockState retourne la BlockState précédente (non nulle = bloc réellement modifié).
     * En 1.21.x le 3e paramètre est int flags (pas boolean isMoving).
     */
    @Inject(
        method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;",
        at = @At("RETURN")
    )
    private void p2p$onSetBlockState(BlockPos pos, BlockState state, int flags,
                                      CallbackInfoReturnable<BlockState> cir) {
        if (cir.getReturnValue() != null) {
            BlockSyncManager.notifyBlockChanged(
                pos.getX(), pos.getY(), pos.getZ(), state, this.level);
        }
    }
}
