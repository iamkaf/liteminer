package com.iamkaf.liteminer.platform;

import com.iamkaf.liteminer.platform.services.IPlatformHelper;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
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
        int rawExperience = state.getExpDrop(level, pos, blockEntity, breaker, tool);
        return EnchantmentHelper.processBlockExperience(level, tool, rawExperience);
    }
}
