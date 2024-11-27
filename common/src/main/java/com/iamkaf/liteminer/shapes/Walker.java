package com.iamkaf.liteminer.shapes;

import com.iamkaf.liteminer.Blacklist;
import com.iamkaf.liteminer.Liteminer;
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
        ItemStack tool = player.getMainHandItem();
        boolean isValidTool =
                !Liteminer.CONFIG.requireCorrectToolEnabled.get() || (!tool.isEmpty() && tool.isCorrectToolForDrops(
                        state));

        return !(Blacklist.isBlacklistedBlock(state) || state.is(Blocks.AIR) || state.liquid() || !isValidTool);
    }
}
