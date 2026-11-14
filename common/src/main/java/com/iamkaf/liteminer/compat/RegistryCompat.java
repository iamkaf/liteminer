package com.iamkaf.liteminer.compat;

//? if >=1.19.3 {
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
//?} else {
/*import net.minecraft.core.Registry;
*///?}
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
//? if <1.20.5 {
/*import net.minecraft.world.effect.MobEffect;
*///?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Registry lookups. Registries moved to {@code BuiltInRegistries} in 1.19.3, and their getters changed in 1.20.5
 * and 1.21.2.
 */
public final class RegistryCompat {
    private RegistryCompat() {
    }

    public static Identifier blockId(Block block) {
        //? if >=1.19.3 {
        return BuiltInRegistries.BLOCK.getKey(block);
        //?} else {
        /*return Registry.BLOCK.getKey(block);
        *///?}
    }

    public static TagKey<Block> blockTag(Identifier id) {
        //? if >=1.19.3 {
        return TagKey.create(Registries.BLOCK, id);
        //?} else {
        /*return TagKey.create(Registry.BLOCK_REGISTRY, id);
        *///?}
    }

    public static TagKey<Item> itemTag(Identifier id) {
        //? if >=1.19.3 {
        return TagKey.create(Registries.ITEM, id);
        //?} else {
        /*return TagKey.create(Registry.ITEM_REGISTRY, id);
        *///?}
    }

    /** Whether the block tag exists and holds at least one block. */
    public static boolean hasBlocks(TagKey<Block> tag) {
        //? if >=1.21.2 {
        var optional = BuiltInRegistries.BLOCK.get(tag);
        //?} else if >=1.19.3 {
        /*var optional = BuiltInRegistries.BLOCK.getTag(tag);
        *///?} else {
        /*var optional = Registry.BLOCK.getTag(tag);
        *///?}
        return optional.isPresent() && optional.get().size() > 0;
    }

    /** Whether the player has the status effect registered under {@code id}. Missing effects count as absent. */
    public static boolean hasEffect(Player player, Identifier id) {
        //? if >=1.21.2 {
        return BuiltInRegistries.MOB_EFFECT.get(id)
                .map(player::hasEffect)
                .orElse(false);
        //?} else if >=1.20.5 {
        /*return BuiltInRegistries.MOB_EFFECT.getHolder(id)
                .map(player::hasEffect)
                .orElse(false);
        *///?} else if >=1.19.3 {
        /*MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(id);
        return effect != null && player.hasEffect(effect);
        *///?} else {
        /*MobEffect effect = Registry.MOB_EFFECT.get(id);
        return effect != null && player.hasEffect(effect);
        *///?}
    }
}
