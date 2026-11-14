package com.iamkaf.liteminer.compat;

import com.iamkaf.amber.api.platform.v1.Platform;
import com.iamkaf.liteminer.api.event.LiteminerEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import xaero.pac.common.server.api.OpenPACServerAPI;

/**
 * Keeps vein mining out of Open Parties and Claims claims. Liteminer changes the extra blocks itself, so OPAC's own
 * hooks only see the block the player clicked.
 */
public final class OpacCompat {
    private OpacCompat() {
    }

    public static void init() {
        if (Platform.isModLoaded("openpartiesandclaims")) {
            LiteminerEvents.ALLOW_BLOCK.register(OpacBridge::allowBlock);
        }
    }

    /**
     * Kept behind a nested class so OPAC types are never resolved when OPAC is absent.
     */
    private static final class OpacBridge {
        private OpacBridge() {
        }

        private static InteractionResult allowBlock(LiteminerEvents.BlockContext context) {
            if (!(context.level() instanceof ServerLevel level)) {
                return InteractionResult.PASS;
            }
            InteractionHand hand = context.hand() != null ? context.hand() : InteractionHand.MAIN_HAND;
            boolean breaking = context.operation() == LiteminerEvents.Operation.BREAK;
            // Skipped blocks stay silent, like spawn protection, so one vein doesn't send a message per block.
            boolean denied = OpenPACServerAPI.get(level.getServer()).getChunkProtection().onBlockInteraction(
                    context.player(), hand, context.tool(), level, context.pos(), null, breaking, false, true);
            return denied ? InteractionResult.FAIL : InteractionResult.PASS;
        }
    }
}
