package com.iamkaf.liteminer.shapes;

import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.tags.TagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;

public interface Walker {
    HashSet<BlockPos> walk(Level level, Player player, BlockPos origin);

    @SuppressWarnings("deprecation")
    default boolean shouldMine(Player player, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        // air and liquids
        if (state.is(Blocks.AIR) || state.liquid()) {
            return false;
        }

        // whitelist
        if (TagHelper.isBlockWhitelistEnabled() && !TagHelper.isWhitelistedBlock(state)) {
            return false;
        }

        // unbreakable blocks
        if (state.getDestroySpeed(level, pos) < 0 && !player.isCreative()) {
            return false;
        }

        // tool check
        ItemStack tool = player.getMainHandItem();
        if (!isValidTool(tool, state)) {
            return false;
        }

        // excluded blocks
        return !TagHelper.isExcludedBlock(state);
    }

    default boolean isValidTool(ItemStack tool, BlockState state) {
        if (TagHelper.isExcludedTool(tool)) {
            return false;
        }
        return !Liteminer.CONFIG.requireCorrectToolEnabled.get() || (!tool.isEmpty() && (tool.isCorrectToolForDrops(state) || TagHelper.isIncludedTool(
                tool)));
    }
}
