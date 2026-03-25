package com.iamkaf.liteminer.neoforge;

import com.iamkaf.liteminer.Constants;
import com.iamkaf.liteminer.LiteminerClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class LiteminerNeoForgeClient {
    public LiteminerNeoForgeClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        container.registerConfig(ModConfig.Type.CLIENT, LiteminerClient.CONFIG_SPEC);
        LiteminerClient.init();
    }
}
