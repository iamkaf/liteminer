package com.iamkaf.liteminer.compat;

import com.iamkaf.amber.api.platform.v1.Platform;
import com.iamkaf.liteminer.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.11 {
import net.minecraft.world.level.gamerules.GameRules;
//?} else {
/*import net.minecraft.world.level.GameRules;
*///?}
//? if >=1.20 {
import net.minecraft.world.level.storage.loot.LootParams;
//?} else {
/*import net.minecraft.world.level.storage.loot.LootContext;
*///?}

import java.util.Objects;

/** Player block breaking: tool wear, drops, and experience. The calls changed in 1.19.4, 1.20, 1.21, and 1.21.11. */
public final class BlockBreakCompat {
    private BlockBreakCompat() {
    }

    /** Whether the world's block drops game rule is on. */
    public static boolean blockDropsEnabled(ServerLevel level) {
        //? if >=1.21.11 {
        return level.getGameRules().get(GameRules.BLOCK_DROPS);
        //?} else {
        /*return level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS);
        *///?}
    }

    /** Wears the player's main-hand tool by one use. */
    public static void damageTool(ItemStack tool, Player player) {
        //? if >=1.21 {
        tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        //?} else {
        /*tool.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        *///?}
    }

    /**
     * Drops what breaking {@code state} with {@code tool} would, at {@code dropPos}. Experience follows the
     * loader: Fabric drops it from {@code spawnAfterBreak}, so does Forge since 1.20, and NeoForge and older Forge
     * award it separately.
     */
    public static void dropLoot(ServerLevel level, Player player, BlockPos pos, BlockPos dropPos, BlockState state,
            ItemStack tool) {
        //? if >=1.20 {
        var builder = new LootParams.Builder(level);
        //?} else {
        /*var builder = new LootContext.Builder(level);
        *///?}
        builder.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, tool)
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.THIS_ENTITY, player);
        if (state.hasBlockEntity()) {
            builder.withParameter(LootContextParams.BLOCK_ENTITY, Objects.requireNonNull(level.getBlockEntity(pos)));
        }

        if (awardsExperienceSeparately()) {
            spawnAfterBreak(state, level, dropPos, tool, false);
            int xp = Services.PLATFORM.getBlockExperience(level, pos, state, level.getBlockEntity(pos), player, tool);
            if (xp > 0 && blockDropsEnabled(level)) {
                ExperienceOrb.award(level, Vec3.atCenterOf(dropPos), xp);
            }
        } else {
            spawnAfterBreak(state, level, dropPos, tool, true);
        }

        for (var stack : state.getDrops(builder)) {
            Block.popResource(level, dropPos, stack);
        }
    }

    /** NeoForge, and Forge before 1.20, moved block XP out of spawnAfterBreak. */
    private static boolean awardsExperienceSeparately() {
        //? if >=1.20 {
        return Platform.isNeoForge();
        //?} else {
        /*return Platform.isNeoForge() || Platform.isForge();
        *///?}
    }

    private static void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool,
            boolean dropExperience) {
        //? if >=1.19.4 {
        state.spawnAfterBreak(level, pos, tool, dropExperience);
        //?} else {
        /*state.spawnAfterBreak(level, pos, tool);
        *///?}
    }

    /** Whether the block is a fluid, which a vein stops at instead of deleting. */
    @SuppressWarnings("deprecation")
    public static boolean isLiquid(BlockState state) {
        //? if >=1.20 {
        return state.liquid();
        //?} else {
        /*return state.getMaterial().isLiquid();
        *///?}
    }
}
