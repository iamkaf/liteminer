package com.iamkaf.liteminer.platform.services;

public interface IPlatformHelper {

    /**
     * Gets the name of the current platform.
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Gets the experience amount that would be dropped when this block is broken.
     * Respects Fortune enchantment, Silk Touch, and other modifiers.
     *
     * @param level The server level
     * @param pos The block position
     * @param state The block state
     * @param blockEntity The block entity (nullable)
     * @param breaker The entity breaking the block (nullable)
     * @param tool The tool being used
     * @return The amount of experience to drop, or 0 if none
     */
    int getBlockExperience(
        net.minecraft.server.level.ServerLevel level,
        net.minecraft.core.BlockPos pos,
        net.minecraft.world.level.block.state.BlockState state,
        net.minecraft.world.level.block.entity.BlockEntity blockEntity,
        net.minecraft.world.entity.Entity breaker,
        net.minecraft.world.item.ItemStack tool
    );
}
