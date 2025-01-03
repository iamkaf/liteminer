package com.iamkaf.liteminer.tags;

import com.iamkaf.liteminer.Liteminer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class LiteminerTags {
    private static TagKey<Item> createItemTag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    private static TagKey<Block> createBlockTag(String namespace, String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    public static class Items {
        public static final TagKey<Item> EXCLUDED_TOOLS = createItemTag(Liteminer.MOD_ID, "excluded_tools");
        public static final TagKey<Item> INCLUDED_TOOLS = createItemTag(Liteminer.MOD_ID, "included_tools");
    }

    public static class Blocks {
        public static final TagKey<Block> EXCLUDED_BLOCKS = createBlockTag(Liteminer.MOD_ID, "excluded_blocks");
        public static final TagKey<Block> BLOCK_WHITELIST = createBlockTag(Liteminer.MOD_ID, "block_whitelist");
    }

    public static class Compat {
        public static final TagKey<Item> EXCLUDED_TOOLS = createItemTag("ftbultimine", "excluded_tools");
        public static final TagKey<Item> INCLUDED_TOOLS = createItemTag("ftbultimine", "included_tools");
        public static final TagKey<Block> EXCLUDED_BLOCKS = createBlockTag("ftbultimine", "excluded_blocks");
        public static final TagKey<Block> BLOCK_WHITELIST = createBlockTag("ftbultimine", "block_whitelist");
    }
}
