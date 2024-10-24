package com.iamkaf.liteminer.neoforge;

import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.rendering.BlockHighlightRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

@EventBusSubscriber(modid = Liteminer.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class LiteminerNeoForgeClient {
    @SubscribeEvent
    public static void renderBlockHighlights(RenderHighlightEvent.Block event) {
        event.setCanceled(!BlockHighlightRenderer.renderLiteminerHighlight(event.getPoseStack()));
    }
}

