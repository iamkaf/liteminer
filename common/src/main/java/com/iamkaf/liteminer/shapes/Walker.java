package com.iamkaf.liteminer.shapes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashSet;

public interface Walker {
    HashSet<BlockPos> walk(Level level, Player player, BlockPos origin);
}
