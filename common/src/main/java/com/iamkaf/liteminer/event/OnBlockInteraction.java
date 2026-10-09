package com.iamkaf.liteminer.event;

import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.LiteminerPlayerState;
import com.iamkaf.liteminer.api.event.LiteminerEvents;
import com.iamkaf.liteminer.api.shape.LiteminerShape;
import com.iamkaf.liteminer.api.shape.LiteminerShapes;
import com.iamkaf.liteminer.shapes.VeinmineChecks;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OnBlockInteraction {
    private static boolean interacting;

    /**
     * Runs a vein interaction around an item use on a block. The clicked block is used first, and the
     * vein only follows when that use consumed the action.
     *
     * @return the clicked block's result, or {@code null} to let the use run normally
     */
    public static @Nullable InteractionResult useOn(ItemStack tool, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        // Interactions inside the vein, including the clicked block's own use, run normally.
        if (interacting || level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return null;
        }

        if (!isTieredItem(tool.getItem())) {
            return null;
        }

        LiteminerPlayerState playerState = Liteminer.instance.getPlayerState(serverPlayer);

        if (!playerState.getKeymappingState()) {
            return null;
        }

        if (FoodExhaustion.isTooHungry(player)) {
            return null;
        }

        // 1 durability left on the tool
        if (tool.isDamageableItem() && (tool.getMaxDamage() - tool.getDamageValue()) == 1) {
            return null;
        }

        interacting = true;
        try {
            return interact(player, level, playerState, tool, context);
        } finally {
            interacting = false;
        }
    }

    private static InteractionResult interact(Player player, Level level, LiteminerPlayerState playerState,
            ItemStack tool, UseOnContext context) {
        InteractionHand hand = context.getHand();
        BlockPos blockPos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        Item item = tool.getItem();
        int shapeIndex = playerState.getShape();
        LiteminerShape shape = LiteminerShapes.byIndex(shapeIndex).orElseThrow();
        int blockLimit = Liteminer.CONFIG.blockBreakLimit.get();
        BlockState originState = level.getBlockState(blockPos);
        BlockEntity originBlockEntity = level.getBlockEntity(blockPos);
        // Walk before the use changes the clicked block, since shapeless matches its current state.
        var blocks = shape.walk(level, player, blockPos)
                .stream()
                .sorted(Comparator.comparingInt(p -> p.distManhattan(blockPos)))
                .toList();

        InteractionResult originResult = tool.useOn(context);
        if (!originResult.consumesAction()) {
            return originResult;
        }

        InteractionResult startResult = LiteminerEvents.BEFORE_VEINMINE.invoker().beforeVeinmine(
                new LiteminerEvents.StartContext(
                        LiteminerEvents.Operation.INTERACT,
                        level,
                        player,
                        hand,
                        blockPos,
                        originState,
                        originBlockEntity,
                        tool,
                        shape,
                        shapeIndex,
                        blockLimit
                )
        );
        if (startResult != InteractionResult.PASS) {
            return originResult;
        }

        List<BlockPos> processed = new ArrayList<>();
        List<BlockPos> skipped = new ArrayList<>();
        int processedIncludingOrigin = 1;

        for (var block : blocks) {
            if (block.equals(blockPos)) {
                continue;
            }
            if (processedIncludingOrigin >= blockLimit) {
                break;
            }
            if (!VeinmineChecks.shouldMine(player, level, block)) {
                skipped.add(block);
                continue;
            }

            // A broken tool stops the vein. Before 26.3, axes still strip once their stack is empty.
            if (tool.isEmpty()) {
                break;
            }
            if (tool.isDamageableItem()) {
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
                            originBlockEntity,
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
                    item.useOn(new UseOnContext(player, hand, new BlockHitResult(Vec3.atBottomCenterOf(block), direction, block, false)));
            if (!interactionResult.consumesAction()) {
                skipped.add(block);
                continue;
            }

            processed.add(block);
            processedIncludingOrigin++;

            FoodExhaustion.apply(player);
        }

        LiteminerEvents.AFTER_VEINMINE.invoker().afterVeinmine(new LiteminerEvents.ResultContext(
                LiteminerEvents.Operation.INTERACT,
                level,
                player,
                hand,
                blockPos,
                originState,
                originBlockEntity,
                tool,
                shape,
                shapeIndex,
                blockLimit,
                blocks,
                processed,
                skipped
        ));

        return originResult;
    }

    private static boolean isTieredItem(Item item) {
        // the toolness of the tools now comes from a tool data component
        var tool = item.getDefaultInstance().get(DataComponents.TOOL);
        return tool != null;
    }
}
