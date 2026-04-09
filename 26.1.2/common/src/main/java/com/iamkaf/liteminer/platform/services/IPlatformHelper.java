package com.iamkaf.liteminer.platform.services;

public interface IPlatformHelper {

    /**
     * Gets the name of the current platform.
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     *
     * @return True if in a development environment, false otherwise.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Returns the root directory for configuration files.
     *
     * @return The config directory path.
     */
    java.nio.file.Path getConfigDirectory();

    /**
     * Gets the name of the environment type as a string.
     *
     * @return The name of the environment type.
     */
    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }

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
