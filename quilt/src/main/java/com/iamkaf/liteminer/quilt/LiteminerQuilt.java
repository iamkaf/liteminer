package com.iamkaf.liteminer.quilt;

import com.iamkaf.liteminer.Liteminer;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.minecraftforge.fml.config.ModConfig;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;

public final class LiteminerQuilt implements ModInitializer {
    @Override
    public void onInitialize(ModContainer mod) {
        new Liteminer();
        Liteminer.init();
        ForgeConfigRegistry.INSTANCE.register(Liteminer.MOD_ID, ModConfig.Type.COMMON, Liteminer.CONFIG_SPEC);
    }
}
