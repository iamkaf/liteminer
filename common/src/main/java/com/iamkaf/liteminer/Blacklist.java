package com.iamkaf.liteminer;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

public class Blacklist {
    private static Set<Block> blacklist = Set.of();

    public static boolean isBlacklistedBlock(BlockState state) {
        return blacklist.contains(state.getBlock());
    }
}
