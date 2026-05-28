package com.p2pminecraft.mixin;

import com.p2pminecraft.runtime.BlockSyncManager;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

    // setBlockState → obfusqué "a(Lis;Leoh;I)Leoh;" dans eqq (client-mappings-1.21.11)
    //
    // @Coerce : le bytecode compilé du mixin n'est pas remappé automatiquement (IRemapper
    // ne touche pas le bytecode des mixins eux-mêmes, seulement les cibles). Sans @Coerce,
    // Mixin valide que le descripteur du handler correspond exactement au type obfusqué
    // → InvalidInjectionException "Expected Lis; but found Lnet/minecraft/core/BlockPos;".
    // Avec @Coerce Object, Mixin accepte tout type Object compatible, puis génère un cast
    // implicite à l'injection. Les coords sont extraites dans BlockSyncManager par réflexion.
    @Inject(method = "a(Lis;Leoh;I)Leoh;", at = @At("RETURN"))
    private void p2p$onSetBlockState(
            @Coerce Object pos,
            @Coerce Object state,
            int flags,
            CallbackInfoReturnable<?> cir) {
        if (cir.getReturnValue() != null) {
            BlockSyncManager.notifyBlockChanged(
                pos, state, BlockSyncManager.getStoredLevel());
        }
    }
}
