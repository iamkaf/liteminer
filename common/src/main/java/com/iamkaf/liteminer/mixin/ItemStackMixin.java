package com.iamkaf.liteminer.mixin;

import com.iamkaf.liteminer.event.OnBlockInteraction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    // Vanilla reaches this only after the clicked block declined the click, so vein interactions follow
    // the item use instead of every right-click.
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void liteminer$useOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = OnBlockInteraction.useOn((ItemStack) (Object) this, context);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
