package com.iamkaf.liteminer.neoforge;

import com.iamkaf.liteminer.Liteminer;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;

@Mod(Liteminer.MOD_ID)
public final class LiteminerNeoForge {
    public LiteminerNeoForge(IEventBus eBussy, ModContainer container) {
        new Liteminer();
        Liteminer.init();
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
