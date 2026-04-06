package com.iamkaf.liteminer.fabric;

import com.iamkaf.liteminer.Liteminer;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraftforge.fml.config.ModConfig;

public final class LiteminerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        new Liteminer();
        Liteminer.init();
        ForgeConfigRegistry.INSTANCE.register(Liteminer.MOD_ID, ModConfig.Type.COMMON, Liteminer.CONFIG_SPEC);
    }
}
