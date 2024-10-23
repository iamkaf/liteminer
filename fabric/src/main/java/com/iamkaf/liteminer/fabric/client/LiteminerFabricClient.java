package com.iamkaf.liteminer.fabric.client;

import com.iamkaf.liteminer.rendering.BlockHighlightRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public final class LiteminerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WorldRenderEvents.BLOCK_OUTLINE.register((worldRenderContext, blockOutlineContext) -> BlockHighlightRenderer.renderLiteminerHighlight(
                worldRenderContext.matrixStack()));
    }
}
