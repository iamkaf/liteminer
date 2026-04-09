package com.iamkaf.liteminer.fabric.datagen;

import com.iamkaf.liteminer.tags.LiteminerTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider {
    public ModBlockTagProvider(FabricDataOutput output,
            CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        getOrCreateTagBuilder(LiteminerTags.Blocks.BLOCK_WHITELIST);
        getOrCreateTagBuilder(LiteminerTags.Blocks.EXCLUDED_BLOCKS).add(Blocks.BEDROCK);
    }
}
