package com.iamkaf.liteminer.fabric;

import net.fabricmc.api.ModInitializer;

import com.iamkaf.liteminer.Liteminer;

public final class LiteminerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        Liteminer.init();
    }
}
