package com.iamkaf.liteminer.neoforge;

import com.iamkaf.liteminer.Constants;
import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.LiteminerMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(Constants.MOD_ID)
public final class LiteminerNeoForge {
    public LiteminerNeoForge(IEventBus eventBus, ModContainer container) {
        LiteminerMod.init();
        container.registerConfig(ModConfig.Type.COMMON, Liteminer.CONFIG_SPEC);
    }

//    @EventBusSubscriber(modid = Liteminer.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
//    public class Events {
//        @SubscribeEvent
//        public static void onHarvestTime(PlayerEvent.BreakSpeed event) {
//
//        }
//    }
}
