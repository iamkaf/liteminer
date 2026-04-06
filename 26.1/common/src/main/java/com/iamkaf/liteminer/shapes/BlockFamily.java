package com.iamkaf.liteminer.shapes;

import com.iamkaf.liteminer.Liteminer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class BlockFamily {
    private static final Map<Block, Set<Block>> BLOCK_MATCHES = new HashMap<>();

    static {
        // Vanilla ore variants
        makeFamily(Blocks.RAW_IRON_BLOCK, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
        makeFamily(Blocks.RAW_GOLD_BLOCK, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE);
        makeFamily(Blocks.RAW_COPPER_BLOCK, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);

        makeFamily(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
        makeFamily(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        makeFamily(Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        makeFamily(Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
        makeFamily(Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
    }

    /**
     * Determines if two blocks are considered a match based on predefined family relationships.
     * Blocks are considered a match if they are either:
     * 1. The same block (identity check).
     * 2. Belong to the same "family" of blocks as defined in the BLOCK_MATCHES map.
     *
     * @param from The block being checked against.
     * @param to   The block to compare with.
     * @return true if the blocks are either the same or part of the same family, false otherwise.
     */
    public static boolean matches(Block from, Block to) {
        if (to.equals(from)) {
            return true;
        }

        if (Liteminer.CONFIG.matchDeepslateOreVariants.get() && matchesDeepslateOreVariant(from, to)) {
            return true;
        }

        if (BLOCK_MATCHES.containsKey(to)) {
            var blockMatches = BLOCK_MATCHES.get(to);
            return blockMatches.contains(from);
        }

        if ((from instanceof FlowerBlock || from instanceof TallGrassBlock || from instanceof DoublePlantBlock) && (to instanceof FlowerBlock || to instanceof TallGrassBlock || to instanceof DoublePlantBlock)) {
            return true;
        }

        return false;
    }

    public static boolean matches(BlockState from, BlockState to) {
        if (!matches(from.getBlock(), to.getBlock())) {
            return false;
        }

        if (!Liteminer.CONFIG.distinguishGrownCrops.get()) {
            return true;
        }

        if (!from.getBlock().equals(to.getBlock())) {
            return true;
        }

        Optional<IntegerProperty> growthProperty = getGrowthProperty(from);
        if (growthProperty.isEmpty()) {
            return true;
        }

        IntegerProperty property = growthProperty.get();
        return isGrown(from, property) == isGrown(to, property);
    }

    private static boolean isGrown(BlockState state, IntegerProperty property) {
        int value = state.getValue(property);
        int max = property.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(value);
        return value >= max;
    }

    private static Optional<IntegerProperty> getGrowthProperty(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty integerProperty) {
                String name = integerProperty.getName();
                if ("age".equals(name) || "stage".equals(name)) {
                    return Optional.of(integerProperty);
                }
            }
        }
        return Optional.empty();
    }

    private static boolean matchesDeepslateOreVariant(Block from, Block to) {
        Identifier fromId = BuiltInRegistries.BLOCK.getKey(from);
        Identifier toId = BuiltInRegistries.BLOCK.getKey(to);

        // If either ID is missing, don't try to be clever.
        if (fromId == null || toId == null) {
            return false;
        }

        String fromPath = fromId.getPath();
        String toPath = toId.getPath();

        // Rule: <name>_ore === deepslate_<name>_ore
        if (!fromPath.endsWith("_ore") || !toPath.endsWith("_ore")) {
            return false;
        }

        String fromBase = fromPath.startsWith("deepslate_") ? fromPath.substring("deepslate_".length()) : fromPath;
        String toBase = toPath.startsWith("deepslate_") ? toPath.substring("deepslate_".length()) : toPath;

        if (!fromBase.equals(toBase)) {
            return false;
        }

        boolean fromIsDeepslate = fromPath.startsWith("deepslate_");
        boolean toIsDeepslate = toPath.startsWith("deepslate_");

        return fromIsDeepslate != toIsDeepslate;
    }

    /**
     * Creates a "family" of blocks that match with each other.
     * This method ensures that each block in the provided list can be matched with every other block in
     * the list.
     *
     * @param blocks The blocks that belong to the same family and should match with each other.
     */
    private static void makeFamily(Block... blocks) {
        for (Block block : blocks) {
            BLOCK_MATCHES.put(block, Set.of(blocks));
        }
    }
}
