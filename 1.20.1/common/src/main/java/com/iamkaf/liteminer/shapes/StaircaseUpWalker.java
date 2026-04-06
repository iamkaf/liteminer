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
        Direction direction = player.getDirection();
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

        while (blocksToCollapse.size() < Liteminer.CONFIG.blockBreakLimit.get()) {
            boolean shouldMineCursor = shouldMine(player, level, cursor);
            boolean shouldMineAboveCursor = shouldMine(player, level, cursor.above());
            if (!shouldMineCursor && !shouldMineAboveCursor) {
                break;
            }
            if (shouldMineCursor) {
                blocksToCollapse.add(cursor);
            }
            if (shouldMineAboveCursor) {
                blocksToCollapse.add(cursor.below());
            }
            cursor = cursor.relative(direction).above();
        }

        blocksToCollapse.add(myPos);
    }
}
