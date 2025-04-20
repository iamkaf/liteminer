package com.iamkaf.liteminer.fabric;

import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.fabricmc.api.ModInitializer;

import com.iamkaf.liteminer.Liteminer;
import net.neoforged.fml.config.ModConfig;

public final class LiteminerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        new Liteminer();
        Liteminer.init();
        ConfigRegistry.INSTANCE.register(Liteminer.MOD_ID, ModConfig.Type.COMMON, Liteminer.CONFIG_SPEC);
    }
}
