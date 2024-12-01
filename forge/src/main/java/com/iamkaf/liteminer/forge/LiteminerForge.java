package com.iamkaf.liteminer.forge;

import com.iamkaf.liteminer.Liteminer;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Liteminer.MOD_ID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LiteminerForge {
    public LiteminerForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(Liteminer.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        new Liteminer();
        Liteminer.init();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Liteminer.CONFIG_SPEC);
    }
}
