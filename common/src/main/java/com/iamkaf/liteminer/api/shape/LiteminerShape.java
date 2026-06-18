package com.iamkaf.liteminer.api.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashSet;

/**
 * A registered Liteminer mining shape.
 *
 * @param id          stable namespaced id for the shape
 * @param displayName text shown in the Liteminer HUD and shape change messages
 * @param walker      candidate block provider for the shape
 */
public record LiteminerShape(Identifier id, Component displayName, ShapeWalker walker) {
    /**
     * Calculates candidate positions for this shape.
     *
     * @param level  the level where the shape is being evaluated
     * @param player the player using Liteminer
     * @param origin the block that started the operation
     * @return candidate block positions for this shape
     */
    public HashSet<BlockPos> walk(Level level, Player player, BlockPos origin) {
        return walker.walk(level, player, origin);
    }

    /**
     * Returns the plain text display name for logs and legacy string consumers.
     *
     * @return the display name as a plain string
     */
    @Override
    public String toString() {
        return displayName.getString();
    }
}
