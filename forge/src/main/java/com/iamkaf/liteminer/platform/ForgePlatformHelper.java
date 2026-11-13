package com.iamkaf.liteminer.platform;

import com.iamkaf.liteminer.platform.services.IPlatformHelper;

public class ForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Forge";
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
        //? if >=1.21 {
        var enchantments = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        int fortuneLevel = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
            enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE),
            tool
        );
        int silkTouchLevel = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
            enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),
            tool
        );
        //?} else {
        /*int fortuneLevel = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
            net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE,
            tool
        );
        int silkTouchLevel = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
            net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,
            tool
        );
        *///?}
        //? if >=1.19 {
        return state.getExpDrop(level, level.getRandom(), pos, fortuneLevel, silkTouchLevel);
        //?} else {
        /*return state.getExpDrop(level, pos, fortuneLevel, silkTouchLevel);
        *///?}
    }
}
