package com.iamkaf.liteminer.mixin;

import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.LiteminerClient;
import com.iamkaf.liteminer.shapes.ShapelessWalker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
        throw new IllegalStateException("wat?");
    }

    @Inject(method = "getDestroySpeed", at = @At("TAIL"), cancellable = true)
    public void getDestroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        if (!Liteminer.CONFIG.harvestTimePerBlockModifierEnabled.get()) {
            return;
        }

        Float originalSpeed = cir.getReturnValue();
        if (!level().isClientSide) {
            var speedModifier = Liteminer.instance.onBreakSpeed((ServerPlayer) (Object) this, originalSpeed);
            if (speedModifier != originalSpeed) {
                cir.setReturnValue(originalSpeed * speedModifier);
            }
        } else {
            if (LiteminerClient.isVeinMining()) {
                cir.setReturnValue(originalSpeed * Liteminer.getScaledBreakSpeedModifier(
                        liteminer$calculateBlockCountForClient()));
            }
        }
    }

    @Unique
    private int liteminer$calculateBlockCountForClient() {
        ShapelessWalker shapelessWalker = new ShapelessWalker();
        Player player = (Player) (Object) this;
        return shapelessWalker.walk(level(), player, ShapelessWalker.raytrace(level(), player).getBlockPos()).size();
    }
}
