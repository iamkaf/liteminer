package com.iamkaf.liteminer;

import net.fabricmc.api.ModInitializer;

/**
 * Fabric entry point.
 */
public class LiteminerFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        LiteminerMod.init();
    }
}
