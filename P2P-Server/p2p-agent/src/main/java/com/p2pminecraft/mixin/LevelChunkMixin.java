package com.p2pminecraft.mixin;

import com.p2pminecraft.runtime.BlockSyncManager;
import com.p2pminecraft.runtime.DistributedChunkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

    @Shadow public Level level;

    @Shadow public abstract ChunkPos getPos();

    /**
     * HEAD : enregistre le niveau, annule le tick si shouldTickChunk = false.
     * On appelle afterChunkTick avant d'annuler pour que les flushes se produisent.
     */
    @Inject(
        method = "tick(Ljava/util/function/BooleanSupplier;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void p2p$tickHead(BooleanSupplier isGameRunning, CallbackInfo ci) {
        ChunkPos pos = this.getPos();
        int cx = pos.x, cz = pos.z;
        BlockSyncManager.registerLevel(this.level);
        if (!DistributedChunkManager.shouldTickChunk(cx, cz)) {
            DistributedChunkManager.afterChunkTick(cx, cz);
            ci.cancel();
        }
    }

    /**
     * RETURN : appelé uniquement quand le tick s'est déroulé normalement
     * (non annulé par HEAD). Déclenche les flushes P2P.
     */
    @Inject(
        method = "tick(Ljava/util/function/BooleanSupplier;)V",
        at = @At("RETURN")
    )
    private void p2p$tickReturn(BooleanSupplier isGameRunning, CallbackInfo ci) {
        ChunkPos pos = this.getPos();
        DistributedChunkManager.afterChunkTick(pos.x, pos.z);
    }

    /**
     * Détecte chaque changement de bloc local et le diffuse aux pairs.
     * Retourne la BlockState précédente (non nulle = bloc réellement modifié).
     */
    @Inject(
        method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
        at = @At("RETURN")
    )
    private void p2p$onSetBlockState(BlockPos pos, BlockState state, boolean isMoving,
                                      CallbackInfoReturnable<BlockState> cir) {
        if (cir.getReturnValue() != null) {
            BlockSyncManager.notifyBlockChanged(
                pos.getX(), pos.getY(), pos.getZ(), state, this.level);
        }
    }
}
