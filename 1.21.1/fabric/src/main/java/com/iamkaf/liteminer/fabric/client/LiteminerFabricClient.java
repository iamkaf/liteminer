package com.iamkaf.liteminer.fabric.client;

import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.LiteminerClient;
import com.iamkaf.liteminer.rendering.BlockHighlightRenderer;
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.client.ConfigScreenFactoryRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

public final class LiteminerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LiteminerClient.init();

        WorldRenderEvents.BLOCK_OUTLINE.register((worldRenderContext, blockOutlineContext) -> BlockHighlightRenderer.renderLiteminerHighlight(
                worldRenderContext.matrixStack()));

        ConfigScreenFactoryRegistry.INSTANCE.register(Liteminer.MOD_ID, ConfigurationScreen::new);
        NeoForgeConfigRegistry.INSTANCE.register(Liteminer.MOD_ID, ModConfig.Type.CLIENT, LiteminerClient.CONFIG_SPEC);
    }
}
