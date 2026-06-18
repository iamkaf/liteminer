package com.iamkaf.liteminer.api.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashSet;

/**
 * Calculates the block positions included by a Liteminer shape.
 */
@FunctionalInterface
public interface ShapeWalker {
    /**
     * Calculates candidate positions for a veinmine operation.
     *
     * <p>The returned set should usually include {@code origin}. Liteminer skips the origin
     * when processing secondary blocks, but including it keeps highlighting and count behavior
     * consistent with built-in shapes.</p>
     *
     * @param level  the level where the shape is being evaluated
     * @param player the player using Liteminer
     * @param origin the block that started the operation
     * @return candidate block positions for the shape
     */
    HashSet<BlockPos> walk(Level level, Player player, BlockPos origin);
}
