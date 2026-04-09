package com.iamkaf.liteminer.platform;

import com.iamkaf.liteminer.platform.services.IPlatformHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }

    @Override
    public java.nio.file.Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public int getBlockExperience(
        net.minecraft.server.level.ServerLevel level,
        net.minecraft.core.BlockPos pos,
        net.minecraft.world.level.block.state.BlockState state,
        net.minecraft.world.level.block.entity.BlockEntity blockEntity,
        net.minecraft.world.entity.Entity breaker,
        net.minecraft.world.item.ItemStack tool
    ) {
        // NeoForge has getExpDrop method on BlockState
        return state.getExpDrop(level, pos, blockEntity, breaker, tool);
    }
}
