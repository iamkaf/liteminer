package com.iamkaf.liteminer.shapes;

import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.api.shape.ShapeWalker;
import com.iamkaf.liteminer.tags.TagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;

/**
 * Shared candidate rules applied by built-in walkers and the server processing
 * boundary so registered shapes cannot bypass exclusions, tool policy, or the
 * configured block limit.
 */
public final class VeinmineChecks {
    private VeinmineChecks() {}

    @SuppressWarnings("deprecation")
    public static boolean shouldMine(Player player, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        // Stop a tunnel at water instead of deleting the liquid.
        if (state.is(Blocks.AIR) || state.liquid()) {
            return false;
        }

        // An empty whitelist is ignored. A populated one is an allow-list for every shape.
        if (TagHelper.isBlockWhitelistEnabled() && !TagHelper.isWhitelistedBlock(state)) {
            return false;
        }

        // Negative destroy speed is bedrock and similar. Creative still breaks those.
        if (state.getDestroySpeed(level, pos) < 0 && !player.isCreative()) {
            return false;
        }

        ItemStack tool = player.getMainHandItem();
        if (!isValidTool(tool, state)) {
            return false;
        }

        return !TagHelper.isExcludedBlock(state);
    }

    private static boolean isValidTool(ItemStack tool, BlockState state) {
        if (TagHelper.isExcludedTool(tool)) {
            return false;
        }
        if (!Liteminer.CONFIG.requireCorrectToolEnabled.get()) {
            return true;
        }
        // Dirt still passes. It does not require a tool for drops.
        if (!state.requiresCorrectToolForDrops()) {
            return true;
        }
        if (tool.isEmpty()) {
            return false;
        }
        // The included-tool tag covers odd cases like a hoe on a custom block.
        return tool.isCorrectToolForDrops(state) || TagHelper.isIncludedTool(tool);
    }

    /**
     * Geometric shapes excavate mixed materials, but harvest time follows the
     * origin. Instant-break blocks such as torches must not expand a tunnel,
     * staircase, or 3x3 into surrounding structure.
     */
    public static ShapeWalker withoutInstantBreakExpansion(ShapeWalker walker) {
        return (level, player, origin) -> {
            BlockState state = level.getBlockState(origin);
            if (state.is(Blocks.AIR) || canExpandGeometrically(level, origin)) {
                return walker.walk(level, player, origin);
            }
            HashSet<BlockPos> originOnly = new HashSet<>();
            originOnly.add(origin);
            return originOnly;
        };
    }

    @SuppressWarnings("deprecation")
    private static boolean canExpandGeometrically(Level level, BlockPos origin) {
        // 0 is torch, flower, redstone. Mixed stone and dirt still expand because both are > 0.
        return level.getBlockState(origin).getDestroySpeed(level, origin) > 0.0f;
    }
}
