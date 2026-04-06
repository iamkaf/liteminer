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

import java.util.HashSet;

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

        // Pre-allocate fixed-size array to avoid ArrayList overhead
        BlockPos[] positions = new BlockPos[9];

        if (direction.equals(Direction.UP) || direction.equals(Direction.DOWN)) {
            positions[0] = myPos.north();
            positions[1] = myPos.north().east();
            positions[2] = myPos.north().west();
            positions[3] = myPos.east();
            positions[4] = myPos;
            positions[5] = myPos.west();
            positions[6] = myPos.south();
            positions[7] = myPos.south().east();
            positions[8] = myPos.south().west();
        } else {
            positions[0] = myPos.relative(direction.getCounterClockWise()).above();
            positions[1] = myPos.above();
            positions[2] = myPos.relative(direction.getClockWise()).above();
            positions[3] = myPos.relative(direction.getCounterClockWise());
            positions[4] = myPos;
            positions[5] = myPos.relative(direction.getClockWise());
            positions[6] = myPos.relative(direction.getCounterClockWise()).below();
            positions[7] = myPos.below();
            positions[8] = myPos.relative(direction.getClockWise()).below();
        }

        for (BlockPos position : positions) {
            if (shouldMine(player, level, position)) {
                blocksToCollapse.add(position);
            }
        }

        blocksToCollapse.add(myPos);
    }
}
