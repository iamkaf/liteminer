package com.iamkaf.liteminer.event;

import com.iamkaf.amber.api.event.v1.events.common.BlockEvents;
import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.LiteminerPlayerState;
import com.iamkaf.liteminer.api.event.LiteminerEvents;
import com.iamkaf.liteminer.api.shape.LiteminerShape;
import com.iamkaf.liteminer.api.shape.LiteminerShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OnBlockInteraction {
    public static void init() {
        BlockEvents.BLOCK_INTERACT.register(OnBlockInteraction::onBlockInteracted);
    }

    private static InteractionResult onBlockInteracted(Player player, Level level, InteractionHand hand, BlockHitResult blockHitResult) {
        Direction direction = blockHitResult.getDirection();
        BlockPos blockPos = blockHitResult.getBlockPos();

        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }

        // Prevents off-hand from interacting when the main hand is already handling this event
        if (hand.equals(InteractionHand.OFF_HAND) && isTieredItem(player.getMainHandItem().getItem())) {
            return InteractionResult.PASS;
        }

        ItemStack tool = player.getItemInHand(hand);
        Item item = tool.getItem();

        if (!isTieredItem(item)) {
            return InteractionResult.PASS;
        }

        LiteminerPlayerState playerState = Liteminer.instance.getPlayerState((ServerPlayer) player);

        if (!playerState.getKeymappingState()) {
            return InteractionResult.PASS;
        }

        // 1 durability left on the tool
        if (tool.isDamageableItem() && (tool.getMaxDamage() - tool.getDamageValue()) == 1) {
            return InteractionResult.PASS;
        }

        int shapeIndex = playerState.getShape();
        LiteminerShape shape = LiteminerShapes.byIndex(shapeIndex).orElseThrow();
        int blockLimit = Liteminer.CONFIG.blockBreakLimit.get();
        BlockState originState = level.getBlockState(blockPos);
        InteractionResult startResult = LiteminerEvents.BEFORE_VEINMINE.invoker().beforeVeinmine(
                new LiteminerEvents.StartContext(
                        LiteminerEvents.Operation.INTERACT,
                        level,
                        player,
                        hand,
                        blockPos,
                        originState,
                        level.getBlockEntity(blockPos),
                        tool,
                        shape,
                        shapeIndex,
                        blockLimit
                )
        );
        if (startResult != InteractionResult.PASS) {
            return startResult;
        }

        var blocks = shape.walk(level, player, blockPos)
                .stream()
                .sorted(Comparator.comparingInt(p -> p.distManhattan(blockPos)))
                .toList();
        List<BlockPos> processed = new ArrayList<>();
        List<BlockPos> skipped = new ArrayList<>();

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

            BlockState state = level.getBlockState(block);
            InteractionResult eventResult = LiteminerEvents.ALLOW_BLOCK.invoker()
                    .allowBlock(new LiteminerEvents.BlockContext(
                            LiteminerEvents.Operation.INTERACT,
                            level,
                            player,
                            hand,
                            blockPos,
                            originState,
                            level.getBlockEntity(blockPos),
                            block,
                            state,
                            level.getBlockEntity(block),
                            tool,
                            shape,
                            shapeIndex,
                            blockLimit
                    ));
            if (eventResult != InteractionResult.PASS) {
                skipped.add(block);
                continue;
            }

            InteractionResult interactionResult =
                    item.useOn(new UseOnContext(player, hand, new BlockHitResult(block.getBottomCenter(), direction, block, false)));
            if (!interactionResult.consumesAction()) {
                skipped.add(block);
                continue;
            }

            processed.add(block);

            boolean exhaustionEnabled = Liteminer.CONFIG.foodExhaustionEnabled.get();
            float exhaustion = Liteminer.CONFIG.foodExhaustion.get().floatValue();
            if (exhaustionEnabled && exhaustion > 0) {
                player.causeFoodExhaustion(exhaustion);
            }
        }

        LiteminerEvents.AFTER_VEINMINE.invoker().afterVeinmine(new LiteminerEvents.ResultContext(
                LiteminerEvents.Operation.INTERACT,
                level,
                player,
                hand,
                blockPos,
                originState,
                level.getBlockEntity(blockPos),
                tool,
                shape,
                shapeIndex,
                blockLimit,
                blocks,
                processed,
                skipped
        ));

        return InteractionResult.PASS;
    }

    private static boolean isTieredItem(Item item) {
        // the toolness of the tools now comes from a tool data component
        var tool = item.getDefaultInstance().get(DataComponents.TOOL);
        return tool != null;
    }
}
