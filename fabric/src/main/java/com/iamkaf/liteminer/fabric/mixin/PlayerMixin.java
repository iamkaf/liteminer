package com.iamkaf.liteminer.fabric.mixin;

import com.iamkaf.liteminer.Liteminer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forge and NeoForge mine through their own break speed event, so only Fabric needs this hook. */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F", at = @At("TAIL"), cancellable = true)
    private void liteminer$getDestroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(Liteminer.scaleBreakSpeed((Player) (Object) this, cir.getReturnValue()));
    }
}
