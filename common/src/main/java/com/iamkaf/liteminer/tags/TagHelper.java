package com.iamkaf.liteminer.tags;

import com.iamkaf.liteminer.Liteminer;
import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class TagHelper {
    private static boolean initialized = false;

    public static void init() {
        LifecycleEvent.SERVER_LEVEL_LOAD.register(TagHelper::report);
    }

    public static boolean isExcludedBlock(BlockState block) {
        return isOr(block, LiteminerTags.Blocks.EXCLUDED_BLOCKS, LiteminerTags.Compat.EXCLUDED_BLOCKS);
    }

    public static boolean isWhitelistedBlock(BlockState block) {
        return isOr(block, LiteminerTags.Blocks.BLOCK_WHITELIST, LiteminerTags.Compat.BLOCK_WHITELIST);
    }

    public static boolean isBlockWhitelistEnabled() {
        return (isBlockTagPopulated(LiteminerTags.Blocks.BLOCK_WHITELIST) || isBlockTagPopulated(LiteminerTags.Compat.BLOCK_WHITELIST));
    }

    public static boolean isExcludedTool(ItemStack stack) {
        return isOr(stack, LiteminerTags.Items.EXCLUDED_TOOLS, LiteminerTags.Compat.EXCLUDED_TOOLS);
    }

    public static boolean isIncludedTool(ItemStack stack) {
        return isOr(stack, LiteminerTags.Items.INCLUDED_TOOLS, LiteminerTags.Compat.INCLUDED_TOOLS);
    }

    private static boolean isBlockTagPopulated(TagKey<Block> tag) {
        var optional = BuiltInRegistries.BLOCK.getTag(tag);
        return optional.isPresent() && optional.get().size() > 0;
    }

    @SafeVarargs
    private static boolean isOr(BlockState block, TagKey<Block>... tags) {
        for (TagKey<Block> tag : tags) {
            if (block.is(tag)) {
                return true;
            }
        }
        return false;
    }

    @SafeVarargs
    private static boolean isOr(ItemStack stack, TagKey<Item>... tags) {
        for (TagKey<Item> tag : tags) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    public static void report(ServerLevel serverLevel) {
        if (initialized) return;
        initialized = true;
        Liteminer.LOGGER.info("Loading Liteminer Tags...");
        BuiltInRegistries.BLOCK.getTag(LiteminerTags.Blocks.BLOCK_WHITELIST).ifPresent(holders -> {
            Liteminer.LOGGER.info("Found " + holders.size() + " blocks [" + LiteminerTags.Blocks.BLOCK_WHITELIST.location() + "]");
        });

        BuiltInRegistries.BLOCK.getTag(LiteminerTags.Blocks.EXCLUDED_BLOCKS).ifPresent(holders -> {
            Liteminer.LOGGER.info("Found " + holders.size() + " blocks [" + LiteminerTags.Blocks.EXCLUDED_BLOCKS.location() + "]");
        });

        BuiltInRegistries.ITEM.getTag(LiteminerTags.Items.EXCLUDED_TOOLS).ifPresent(holders -> {
            Liteminer.LOGGER.info("Found " + holders.size() + " items [" + LiteminerTags.Items.EXCLUDED_TOOLS.location() + "]");
        });

        BuiltInRegistries.ITEM.getTag(LiteminerTags.Items.INCLUDED_TOOLS).ifPresent(holders -> {
            Liteminer.LOGGER.info("Found " + holders.size() + " items [" + LiteminerTags.Items.INCLUDED_TOOLS.location() + "]");
        });
    }
}
