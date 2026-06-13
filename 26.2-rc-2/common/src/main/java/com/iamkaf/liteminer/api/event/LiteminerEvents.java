package com.iamkaf.liteminer.api.event;

import com.iamkaf.amber.api.event.v1.Event;
import com.iamkaf.amber.api.event.v1.EventFactory;
import com.iamkaf.liteminer.api.shape.LiteminerShape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Server-side Liteminer lifecycle events for addon integrations.
 */
public final class LiteminerEvents {
    private LiteminerEvents() {
    }

    /**
     * Fired once before Liteminer processes secondary blocks for a break or interaction.
     *
     * <p>Returning anything other than {@link InteractionResult#PASS} cancels Liteminer's
     * processing for that operation.</p>
     */
    public static final Event<BeforeVeinmine> BEFORE_VEINMINE = EventFactory.createArrayBacked(
            BeforeVeinmine.class,
            callbacks -> context -> {
                for (BeforeVeinmine callback : callbacks) {
                    InteractionResult result = callback.beforeVeinmine(context);
                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }
                return InteractionResult.PASS;
            }
    );

    /**
     * Fired for each secondary block candidate before Liteminer processes it.
     *
     * <p>Returning anything other than {@link InteractionResult#PASS} skips that block and
     * continues processing the remaining candidates.</p>
     */
    public static final Event<AllowBlock> ALLOW_BLOCK = EventFactory.createArrayBacked(
            AllowBlock.class,
            callbacks -> context -> {
                for (AllowBlock callback : callbacks) {
                    InteractionResult result = callback.allowBlock(context);
                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }
                return InteractionResult.PASS;
            }
    );

    /**
     * Fired after Liteminer finishes processing a break or interaction operation.
     */
    public static final Event<AfterVeinmine> AFTER_VEINMINE = EventFactory.createArrayBacked(
            AfterVeinmine.class,
            callbacks -> context -> {
                for (AfterVeinmine callback : callbacks) {
                    callback.afterVeinmine(context);
                }
            }
    );

    /**
     * Callback for whole-operation veinmine checks.
     */
    @FunctionalInterface
    public interface BeforeVeinmine {
        /**
         * Called before any secondary blocks are processed.
         *
         * @param context operation start context
         * @return {@link InteractionResult#PASS} to continue, or any other result to cancel processing
         */
        InteractionResult beforeVeinmine(StartContext context);
    }

    /**
     * Callback for per-block candidate checks.
     */
    @FunctionalInterface
    public interface AllowBlock {
        /**
         * Called before Liteminer processes a secondary block.
         *
         * @param context block candidate context
         * @return {@link InteractionResult#PASS} to process the block, or any other result to skip it
         */
        InteractionResult allowBlock(BlockContext context);
    }

    /**
     * Callback for observing completed veinmine operations.
     */
    @FunctionalInterface
    public interface AfterVeinmine {
        /**
         * Called after Liteminer finishes processing secondary blocks.
         *
         * @param context completed operation context
         */
        void afterVeinmine(ResultContext context);
    }

    /**
     * Type of Liteminer operation being processed.
     */
    public enum Operation {
        /**
         * A block-breaking veinmine operation.
         */
        BREAK,

        /**
         * A block-interaction veinmine operation, such as stripping logs or making paths.
         */
        INTERACT
    }

    /**
     * Context passed before Liteminer begins processing secondary blocks.
     *
     * @param operation         operation type
     * @param level             level where the operation is running
     * @param player            player using Liteminer
     * @param hand              hand used for interaction operations, or {@code null} for break operations
     * @param origin            origin block position
     * @param originState       origin block state
     * @param originBlockEntity origin block entity, if present
     * @param tool              item stack used for the operation
     * @param shape             selected Liteminer shape
     * @param shapeIndex        selected shape index in {@code LiteminerShapes.all()}
     * @param blockLimit        active configured block limit
     */
    public record StartContext(
            Operation operation,
            Level level,
            Player player,
            @Nullable InteractionHand hand,
            BlockPos origin,
            BlockState originState,
            @Nullable BlockEntity originBlockEntity,
            ItemStack tool,
            LiteminerShape shape,
            int shapeIndex,
            int blockLimit
    ) {
    }

    /**
     * Context passed for each secondary block candidate.
     *
     * @param operation         operation type
     * @param level             level where the operation is running
     * @param player            player using Liteminer
     * @param hand              hand used for interaction operations, or {@code null} for break operations
     * @param origin            origin block position
     * @param originState       origin block state
     * @param originBlockEntity origin block entity, if present
     * @param pos               candidate block position
     * @param state             candidate block state
     * @param blockEntity       candidate block entity, if present
     * @param tool              item stack used for the operation
     * @param shape             selected Liteminer shape
     * @param shapeIndex        selected shape index in {@code LiteminerShapes.all()}
     * @param blockLimit        active configured block limit
     */
    public record BlockContext(
            Operation operation,
            Level level,
            Player player,
            @Nullable InteractionHand hand,
            BlockPos origin,
            BlockState originState,
            @Nullable BlockEntity originBlockEntity,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity,
            ItemStack tool,
            LiteminerShape shape,
            int shapeIndex,
            int blockLimit
    ) {
    }

    /**
     * Context passed after Liteminer finishes processing secondary blocks.
     *
     * @param operation         operation type
     * @param level             level where the operation ran
     * @param player            player using Liteminer
     * @param hand              hand used for interaction operations, or {@code null} for break operations
     * @param origin            origin block position
     * @param originState       origin block state
     * @param originBlockEntity origin block entity, if present
     * @param tool              item stack used for the operation
     * @param shape             selected Liteminer shape
     * @param shapeIndex        selected shape index in {@code LiteminerShapes.all()}
     * @param blockLimit        active configured block limit
     * @param candidates        all shape candidates considered by Liteminer, including the origin
     * @param processed         secondary blocks actually processed by Liteminer
     * @param skipped           secondary blocks skipped by {@link #ALLOW_BLOCK}
     */
    public record ResultContext(
            Operation operation,
            Level level,
            Player player,
            @Nullable InteractionHand hand,
            BlockPos origin,
            BlockState originState,
            @Nullable BlockEntity originBlockEntity,
            ItemStack tool,
            LiteminerShape shape,
            int shapeIndex,
            int blockLimit,
            List<BlockPos> candidates,
            List<BlockPos> processed,
            List<BlockPos> skipped
    ) {
        public ResultContext {
            candidates = List.copyOf(candidates);
            processed = List.copyOf(processed);
            skipped = List.copyOf(skipped);
        }
    }
}
