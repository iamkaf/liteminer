package com.iamkaf.liteminer.fabric;

import net.fabricmc.api.ModInitializer;

import com.iamkaf.liteminer.Liteminer;

public final class LiteminerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        new Liteminer();
        Liteminer.init();
    }
}
