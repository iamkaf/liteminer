package com.iamkaf.liteminer.event;

import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.LiteminerPlayerState;
import com.iamkaf.liteminer.shapes.Walker;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Comparator;

import static com.iamkaf.liteminer.Liteminer.WALKERS;

public class OnBlockInteraction {
    public static void init() {
        InteractionEvent.RIGHT_CLICK_BLOCK.register(OnBlockInteraction::onBlockInteracted);
    }

    private static EventResult onBlockInteracted(Player player, InteractionHand hand, BlockPos blockPos,
            Direction direction) {
        Level level = player.level();

        if (level.isClientSide) {
            return EventResult.pass();
        }

        // Prevents off-hand from interacting when the main hand is already handling this event
        if (hand.equals(InteractionHand.OFF_HAND) && isTieredItem(player.getMainHandItem().getItem())) {
            return EventResult.pass();
        }

        ItemStack tool = player.getItemInHand(hand);
        Item item = tool.getItem();

        if (!isTieredItem(item)) {
            return EventResult.pass();
        }

        LiteminerPlayerState playerState = Liteminer.instance.getPlayerState((ServerPlayer) player);

        if (!playerState.getKeymappingState()) {
            return EventResult.pass();
        }

        // 1 durability left on the tool
        if (tool.isDamageableItem() && (tool.getMaxDamage() - tool.getDamageValue()) == 1) {
            return EventResult.pass();
        }

        Walker walker = WALKERS.get(playerState.getShape());

        var blocks = walker.walk(level, player, blockPos)
                .stream()
                .sorted(Comparator.comparingInt(p -> p.distManhattan(blockPos)))
                .toList();

        for (var block : blocks) {
            if (block.equals(blockPos)) {
                continue;
            }

            if (!tool.isEmpty() && tool.isDamageableItem()) {
                boolean itemIsAboutToBreak = tool.getMaxDamage() - tool.getDamageValue() <= 2;
                boolean preventFromBreaking = Liteminer.CONFIG.preventToolBreaking.get();
                if (itemIsAboutToBreak && preventFromBreaking) {
                    break;
                }
            }

            item.useOn(new UseOnContext(player, hand, new BlockHitResult(block.getBottomCenter(), direction, block, false)));

            boolean exhaustionEnabled = Liteminer.CONFIG.foodExhaustionEnabled.get();
            float exhaustion = Liteminer.CONFIG.foodExhaustion.get().floatValue();
            if (exhaustionEnabled && exhaustion > 0) {
                player.causeFoodExhaustion(exhaustion);
            }
        }

        return EventResult.pass();
    }

    private static boolean isTieredItem(Item item) {
        return item instanceof DiggerItem || item instanceof SwordItem;
    }
}
