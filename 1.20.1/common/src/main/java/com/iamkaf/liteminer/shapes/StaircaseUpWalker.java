package com.iamkaf.liteminer.shapes;

import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.tags.TagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public class StaircaseUpWalker implements Walker {
    public final Set<BlockPos> VISITED = new HashSet<>();

    @Override
    public String toString() {
        return "Staircase Up";
    }

    public HashSet<BlockPos> walk(Level level, Player player, BlockPos origin) {
        Direction direction = getStairDirection(level, player);
        HashSet<BlockPos> potentialBrokenBlocks = new HashSet<>();

        potentialBrokenBlocks.add(origin);

        BlockState originState = level.getBlockState(origin);

        if (originState.is(Blocks.AIR)) {
            return new HashSet<>(0);
        }

        if (TagHelper.isExcludedBlock(originState)) {
            return potentialBrokenBlocks;
        }

        searchBlocks(player, level, origin, origin, potentialBrokenBlocks, originState.getBlock(), direction);
        VISITED.clear();

        return potentialBrokenBlocks;
    }

    private void searchBlocks(Player player, Level level, BlockPos myPos, BlockPos absoluteOrigin,
            HashSet<BlockPos> blocksToCollapse, Block originBlock, Direction direction) {
        if (VISITED.size() >= Liteminer.CONFIG.blockBreakLimit.get()) return;
        if (VISITED.contains(myPos)) return;

        BlockState state = level.getBlockState(myPos);

        if (state.is(Blocks.AIR) || TagHelper.isExcludedBlock(state)) return;

        BlockPos cursor = myPos;
        int blockLimit = Liteminer.CONFIG.blockBreakLimit.get();

        while (blocksToCollapse.size() < blockLimit) {
            boolean shouldMineAboveCursor = shouldMine(player, level, cursor.above());
            boolean shouldMineCursor = shouldMine(player, level, cursor);
            boolean shouldMineBelowCursor = shouldMine(player, level, cursor.below());
            if (!shouldMineAboveCursor && !shouldMineCursor && !shouldMineBelowCursor) {
                break;
            }
            if (shouldMineCursor) {
                addIfWithinLimit(blocksToCollapse, cursor, blockLimit);
            }
            if (shouldMineBelowCursor) {
                addIfWithinLimit(blocksToCollapse, cursor.below(), blockLimit);
            }
            if (shouldMineAboveCursor) {
                addIfWithinLimit(blocksToCollapse, cursor.above(), blockLimit);
            }
            cursor = cursor.relative(direction).above();
        }

        blocksToCollapse.add(myPos);
    }

    // Use the mined block face when available so diagonal player yaw does not skew the staircase direction.
    private static Direction getStairDirection(Level level, Player player) {
        Direction direction = TunnelWalker.raytrace(level, player).getDirection().getOpposite();
        if (direction == Direction.UP || direction == Direction.DOWN) {
            return player.getDirection();
        }
        return direction;
    }

    // Stair steps add up to three blocks per iteration, so clamp each insertion to avoid overshooting the limit.
    private static void addIfWithinLimit(HashSet<BlockPos> blocksToCollapse, BlockPos pos, int blockLimit) {
        if (blocksToCollapse.size() >= blockLimit) {
            return;
        }
        blocksToCollapse.add(pos);
    }
}
