package com.iamkaf.liteminer.fabric.client;

import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.LiteminerClient;
import com.iamkaf.liteminer.rendering.BlockHighlightRenderer;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraftforge.fml.config.ModConfig;

public final class LiteminerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LiteminerClient.init();

        WorldRenderEvents.BLOCK_OUTLINE.register((worldRenderContext, blockOutlineContext) -> BlockHighlightRenderer.renderLiteminerHighlight(
                worldRenderContext.matrixStack()));

//        ConfigScreenFactoryRegistry.INSTANCE.register(Liteminer.MOD_ID, ConfigurationScreen::new);

        ForgeConfigRegistry.INSTANCE.register(
                Liteminer.MOD_ID,
                ModConfig.Type.CLIENT,
                LiteminerClient.CONFIG_SPEC
        );
    }
}
