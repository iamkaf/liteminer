package com.iamkaf.liteminer.fabric.datagen;

import com.iamkaf.liteminer.tags.LiteminerTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends FabricTagProvider.ItemTagProvider {
    public ModItemTagProvider(FabricDataOutput output,
            CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        getOrCreateTagBuilder(LiteminerTags.Items.EXCLUDED_TOOLS);
        getOrCreateTagBuilder(LiteminerTags.Items.INCLUDED_TOOLS);
    }
}
