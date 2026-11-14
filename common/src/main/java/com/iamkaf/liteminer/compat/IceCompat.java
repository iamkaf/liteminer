package com.iamkaf.liteminer.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.11 {
import net.minecraft.world.attribute.EnvironmentAttributes;
//?}
//? if >=1.21 {
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.level.block.IceBlock;
//?} else {
/*import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
*///?}

/** Ice breaking. Silk Touch detection changed in 1.21, the evaporation check in 1.21.11, and melting in 26.3. */
public final class IceCompat {
    private IceCompat() {
    }

    /**
     * Applies what vanilla ice does after a player breaks it: melt into water over a solid or liquid block, or
     * disappear in dimensions where water evaporates. Silk Touch keeps the ice intact.
     */
    public static void meltIce(Level level, BlockPos pos, ItemStack tool) {
        if (preventsMelting(tool)) {
            return;
        }

        if (waterEvaporates(level)) {
            level.removeBlock(pos, false);
            return;
        }

        if (shouldMelt(level.getBlockState(pos.below()))) {
            level.setBlockAndUpdate(pos, meltsInto());
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean preventsMelting(ItemStack tool) {
        //? if >=1.21 {
        return EnchantmentHelper.hasTag(tool, EnchantmentTags.PREVENTS_ICE_MELTING);
        //?} else {
        /*return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) != 0;
        *///?}
    }

    private static boolean waterEvaporates(Level level) {
        //? if >=1.21.11 {
        var entry = level.dimensionType().attributes().get(EnvironmentAttributes.WATER_EVAPORATES);
        return entry != null && (Boolean) entry.argument();
        //?} else {
        /*return level.dimensionType().ultraWarm();
        *///?}
    }

    @SuppressWarnings("deprecation")
    private static boolean shouldMelt(BlockState below) {
        //? if >=26.3 {
        return below.is(net.minecraft.tags.BlockTags.ICE_MELTS_WHEN_DESTROYED_ABOVE)
                || BlockBreakCompat.isLiquid(below);
        //?} else if >=1.20 {
        /*return below.blocksMotion() || BlockBreakCompat.isLiquid(below);
        *///?} else {
        /*return below.getMaterial().blocksMotion() || BlockBreakCompat.isLiquid(below);
        *///?}
    }

    private static BlockState meltsInto() {
        //? if >=1.21 {
        return IceBlock.meltsInto();
        //?} else {
        /*return Blocks.WATER.defaultBlockState();
        *///?}
    }
}
