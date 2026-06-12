package com.iamkaf.liteminer.fabric.mixin;

import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.LiteminerClient;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
        throw new IllegalStateException("wat?");
    }

    @Inject(method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F", at = @At("TAIL"), cancellable = true)
    public void liteminer$getDestroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        if (!Liteminer.CONFIG.harvestTimePerBlockModifierEnabled.get()) {
            return;
        }

        Float originalSpeed = cir.getReturnValue();
        if (!level().isClientSide()) {
            var speedModifier = Liteminer.instance.onBreakSpeed((ServerPlayer) (Object) this);
            if (speedModifier != 1f) {
                cir.setReturnValue(originalSpeed * speedModifier);
            }
        } else {
            if (LiteminerClient.isVeinMining()) {
                int blockCount = Liteminer.getSelectedBlockCount(
                        level(),
                        (Player) (Object) this,
                        LiteminerClient.shapes.getCurrentIndex()
                );
                cir.setReturnValue(originalSpeed * Liteminer.getScaledBreakSpeedModifier(blockCount));
            }
        }
    }
}
