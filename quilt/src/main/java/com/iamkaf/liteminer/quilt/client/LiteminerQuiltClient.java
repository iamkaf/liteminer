package com.iamkaf.liteminer.quilt.client;

import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.LiteminerClient;
import com.iamkaf.liteminer.rendering.BlockHighlightRenderer;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraftforge.fml.config.ModConfig;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;

public class LiteminerQuiltClient implements ClientModInitializer {
    @Override
    public void onInitializeClient(ModContainer modContainer) {
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
