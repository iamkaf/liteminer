package com.iamkaf.liteminer.walker;

import com.iamkaf.liteminer.Constants;
import com.iamkaf.liteminer.Liteminer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Walker {
    public final Set<BlockPos> VISITED = new HashSet<>();
    public final int RANGE = 64;

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

        searchBlocks(level, origin, origin, potentialBrokenBlocks, level.getBlockState(origin).getBlock());
        VISITED.clear();

        return potentialBrokenBlocks;
    }

    private void searchBlocks(Level level, BlockPos myPos, BlockPos absoluteOrigin,
            HashSet<BlockPos> blocksToCollapse, Block originBlock) {
        if (VISITED.size() >= Liteminer.CONFIG.blockBreakLimit.get()) return;
        if (VISITED.contains(myPos)) return;
//        if (!level.getBlockState(myPos).is(originBlock)) return;
        if (!NeighborPredicate.matches(originBlock, level.getBlockState(myPos).getBlock())) return;

        blocksToCollapse.add(myPos);
        VISITED.add(myPos);

        for (var neighborPos : getNeighbors(myPos, absoluteOrigin)) {
            searchBlocks(level, neighborPos, absoluteOrigin, blocksToCollapse, originBlock);
        }
    }

    private List<BlockPos> getNeighbors(BlockPos myPos, BlockPos absoluteOrigin) {
        List<BlockPos> blocks = new ArrayList<>();

        blocks.add(myPos.above());
        blocks.add(myPos.below());
        blocks.add(myPos.north());
        blocks.add(myPos.south());
        blocks.add(myPos.east());
        blocks.add(myPos.west());
        blocks.add(myPos.north().east());
        blocks.add(myPos.north().west());
        blocks.add(myPos.south().east());
        blocks.add(myPos.south().west());

        blocks.add(myPos.above().north());
        blocks.add(myPos.above().south());
        blocks.add(myPos.above().east());
        blocks.add(myPos.above().west());
        blocks.add(myPos.above().north().east());
        blocks.add(myPos.above().north().west());
        blocks.add(myPos.above().south().east());
        blocks.add(myPos.above().south().west());

        blocks.add(myPos.below().north());
        blocks.add(myPos.below().south());
        blocks.add(myPos.below().east());
        blocks.add(myPos.below().west());
        blocks.add(myPos.below().north().east());
        blocks.add(myPos.below().north().west());
        blocks.add(myPos.below().south().east());
        blocks.add(myPos.below().south().west());

        blocks.sort(Comparator.comparingInt(p -> p.distManhattan(absoluteOrigin)));

        return blocks;
    }
}
