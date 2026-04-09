package com.iamkaf.liteminer.fabric;

import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.fabricmc.api.ModInitializer;

import com.iamkaf.liteminer.Constants;
import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.LiteminerMod;
import net.neoforged.fml.config.ModConfig;

public final class LiteminerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        LiteminerMod.init();
        ConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.COMMON, Liteminer.CONFIG_SPEC);
    }
}
