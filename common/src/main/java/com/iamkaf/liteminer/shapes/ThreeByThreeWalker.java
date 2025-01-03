package com.iamkaf.liteminer.shapes;

import com.iamkaf.liteminer.tags.TagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ThreeByThreeWalker implements Walker {
    private static @NotNull BlockHitResult raytrace(Level level, Player player) {
        Vec3 eyePosition = player.getEyePosition();
        Vec3 rotation = player.getViewVector(1);
        double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        Vec3 combined = eyePosition.add(rotation.x * reach, rotation.y * reach, rotation.z * reach);

        return level.clip(new ClipContext(eyePosition, combined, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    @Override
    public String toString() {
        return "3x3";
    }

    public HashSet<BlockPos> walk(Level level, Player player, BlockPos origin) {
        Direction direction = raytrace(level, player).getDirection().getOpposite();
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

        return potentialBrokenBlocks;
    }

    private void searchBlocks(Player player, Level level, BlockPos myPos, BlockPos absoluteOrigin,
            HashSet<BlockPos> blocksToCollapse, Block originBlock, Direction direction) {
        if (!shouldMine(player, level, myPos)) return;

        List<BlockPos> positions = new ArrayList<>();

        if (direction.equals(Direction.UP) || direction.equals(Direction.DOWN)) {
            positions.add(myPos.north());
            positions.add(myPos.north().east());
            positions.add(myPos.north().west());
            positions.add(myPos.east());
            positions.add(myPos);
            positions.add(myPos.west());
            positions.add(myPos.south());
            positions.add(myPos.south().east());
            positions.add(myPos.south().west());
        } else {
            positions.add(myPos.relative(direction.getCounterClockWise()).above());
            positions.add(myPos.above());
            positions.add(myPos.relative(direction.getClockWise()).above());
            positions.add(myPos.relative(direction.getCounterClockWise()));
            positions.add(myPos);
            positions.add(myPos.relative(direction.getClockWise()));
            positions.add(myPos.relative(direction.getCounterClockWise()).below());
            positions.add(myPos.below());
            positions.add(myPos.relative(direction.getClockWise()).below());
        }

        for (var position : positions) {
            if (shouldMine(player, level, position)) {
                blocksToCollapse.add(position);
            }
        }

        blocksToCollapse.add(myPos);
    }
}
