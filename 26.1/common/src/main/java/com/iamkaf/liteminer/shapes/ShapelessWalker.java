package com.iamkaf.liteminer.shapes;

import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.tags.TagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class ShapelessWalker implements Walker {
    public final int RANGE = 64;

    // Pre-allocated neighbor offsets for better performance (26 neighbors)
    private static final BlockPos[] NEIGHBOR_OFFSETS = new BlockPos[]{
            // 6 cardinal directions
            new BlockPos(0, 1, 0), new BlockPos(0, -1, 0),
            new BlockPos(0, 0, -1), new BlockPos(0, 0, 1),
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            // 4 horizontal diagonals (same Y)
            new BlockPos(1, 0, -1), new BlockPos(-1, 0, -1),
            new BlockPos(1, 0, 1), new BlockPos(-1, 0, 1),
            // 8 above diagonals
            new BlockPos(0, 1, -1), new BlockPos(0, 1, 1),
            new BlockPos(1, 1, 0), new BlockPos(-1, 1, 0),
            new BlockPos(1, 1, -1), new BlockPos(-1, 1, -1),
            new BlockPos(1, 1, 1), new BlockPos(-1, 1, 1),
            // 8 below diagonals
            new BlockPos(0, -1, -1), new BlockPos(0, -1, 1),
            new BlockPos(1, -1, 0), new BlockPos(-1, -1, 0),
            new BlockPos(1, -1, -1), new BlockPos(-1, -1, -1),
            new BlockPos(1, -1, 1), new BlockPos(-1, -1, 1)
    };

    public static @NotNull BlockHitResult raytrace(Level level, Player player) {
        Vec3 eyePosition = player.getEyePosition();
        Vec3 rotation = player.getViewVector(1);
        double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        Vec3 combined = eyePosition.add(rotation.x * reach, rotation.y * reach, rotation.z * reach);

        return level.clip(new ClipContext(eyePosition,
                combined,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));
    }

    @Override
    public String toString() {
        return "Shapeless";
    }

    public static @NotNull BlockPos raytraceBlock(Level level, Player player) {
        var rayTraceResult = raytrace(level, player);

        Direction direction = rayTraceResult.getDirection();

        return switch (direction) {
            case DOWN, NORTH, WEST -> rayTraceResult.getBlockPos();
            case UP -> rayTraceResult.getBlockPos().below();
            case SOUTH -> rayTraceResult.getBlockPos().north();
            case EAST -> rayTraceResult.getBlockPos().west();
        };
    }

    public HashSet<BlockPos> walk(Level level, Player player, BlockPos origin) {
        HashSet<BlockPos> potentialBrokenBlocks = new HashSet<>();

        potentialBrokenBlocks.add(origin);

        BlockState originState = level.getBlockState(origin);

        if (originState.is(Blocks.AIR)) {
            return new HashSet<>(0);
        }

        if (TagHelper.isExcludedBlock(originState)) {
            return potentialBrokenBlocks;
        }

        HashSet<BlockPos> visited = new HashSet<>();
        searchBlocks(player, level, origin, origin, potentialBrokenBlocks, originState, visited);

        return potentialBrokenBlocks;
    }

    private void searchBlocks(Player player, Level level, BlockPos myPos, BlockPos absoluteOrigin,
            HashSet<BlockPos> blocksToCollapse, BlockState originState, HashSet<BlockPos> visited) {
        if (visited.size() >= Liteminer.CONFIG.blockBreakLimit.get()) return;
        if (visited.contains(myPos)) return;
        if (!BlockFamily.matches(originState, level.getBlockState(myPos))) return;
        if (!shouldMine(player, level, myPos)) return;

        blocksToCollapse.add(myPos);
        visited.add(myPos);

        for (var neighborPos : getNeighbors(myPos, absoluteOrigin)) {
            searchBlocks(player, level, neighborPos, absoluteOrigin, blocksToCollapse, originState, visited);
        }
    }

    private List<BlockPos> getNeighbors(BlockPos myPos, BlockPos absoluteOrigin) {
        // Use pre-allocated offsets to reduce object allocation
        BlockPos[] neighbors = new BlockPos[NEIGHBOR_OFFSETS.length];
        for (int i = 0; i < NEIGHBOR_OFFSETS.length; i++) {
            neighbors[i] = myPos.offset(NEIGHBOR_OFFSETS[i]);
        }

        // Sort by distance to origin for more intuitive mining order
        Arrays.sort(neighbors, Comparator.comparingInt(p -> p.distManhattan(absoluteOrigin)));

        return Arrays.asList(neighbors);
    }
}
