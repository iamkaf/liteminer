package com.iamkaf.liteminer.fabric;

import net.fabricmc.api.ModInitializer;

import com.iamkaf.liteminer.LiteminerMod;

public final class LiteminerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        LiteminerMod.init();
    }
}
