package com.iamkaf.liteminer.neoforge;

import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.LiteminerClient;
import com.iamkaf.liteminer.rendering.BlockHighlightRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Liteminer.MOD_ID, dist = Dist.CLIENT)
public class LiteminerNeoForgeClient {
    public LiteminerNeoForgeClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        container.registerConfig(ModConfig.Type.CLIENT, LiteminerClient.CONFIG_SPEC);
        LiteminerClient.init();
    }

    @EventBusSubscriber(modid = Liteminer.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public class Events {
        @SubscribeEvent
        public static void renderBlockHighlights(RenderHighlightEvent.Block event) {
            event.setCanceled(!BlockHighlightRenderer.renderLiteminerHighlight(event.getPoseStack()));
        }
    }
}

