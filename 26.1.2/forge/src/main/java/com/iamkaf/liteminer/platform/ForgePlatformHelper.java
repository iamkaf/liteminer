package com.iamkaf.liteminer.platform;

import com.iamkaf.liteminer.platform.services.IPlatformHelper;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.LoadingModList;

public class ForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return LoadingModList.getModFileById(modId) != null;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
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
        var enchantments = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        int fortuneLevel = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
            enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE),
            tool
        );
        int silkTouchLevel = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
            enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),
            tool
        );
        return state.getExpDrop(level, level.getRandom(), pos, fortuneLevel, silkTouchLevel);
    }
}
