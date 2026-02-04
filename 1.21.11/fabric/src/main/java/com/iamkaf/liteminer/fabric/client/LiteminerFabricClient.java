package com.iamkaf.liteminer.fabric.client;

import com.iamkaf.liteminer.Constants;
import com.iamkaf.liteminer.LiteminerClient;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.v5.client.ConfigScreenFactoryRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

public final class LiteminerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LiteminerClient.init();

        ConfigScreenFactoryRegistry.INSTANCE.register(Constants.MOD_ID, ConfigurationScreen::new);
        ConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.CLIENT, LiteminerClient.CONFIG_SPEC);
    }
}
