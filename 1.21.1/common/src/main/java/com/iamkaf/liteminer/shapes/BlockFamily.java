package com.iamkaf.liteminer.shapes;

import net.minecraft.world.level.block.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BlockFamily {
    private static final Map<Block, Set<Block>> BLOCK_MATCHES = new HashMap<>();

    static {
        makeFamily(Blocks.RAW_IRON_BLOCK, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
        makeFamily(Blocks.RAW_GOLD_BLOCK, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE);
        makeFamily(Blocks.RAW_COPPER_BLOCK, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
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

        if (BLOCK_MATCHES.containsKey(to)) {
            var blockMatches = BLOCK_MATCHES.get(to);
            return blockMatches.contains(from);
        }

        if ((from instanceof FlowerBlock || from instanceof TallGrassBlock || from instanceof DoublePlantBlock) && (to instanceof FlowerBlock || to instanceof TallGrassBlock || to instanceof DoublePlantBlock)) {
            return true;
        }

        return false;
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
