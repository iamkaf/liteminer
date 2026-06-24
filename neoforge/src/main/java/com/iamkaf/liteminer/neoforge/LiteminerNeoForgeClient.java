package com.iamkaf.liteminer.neoforge;

import com.iamkaf.konfig.api.v1.KonfigClientScreens;
import com.iamkaf.konfig.neoforge.api.v1.KonfigNeoForgeClientScreens;
import com.iamkaf.liteminer.Constants;
import com.iamkaf.liteminer.LiteminerClient;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class LiteminerNeoForgeClient {
    public LiteminerNeoForgeClient(ModContainer container) {
        KonfigNeoForgeClientScreens.register(container, Constants.MOD_ID);
        LiteminerClient.setOpenConfigScreenCallback(
                () -> Minecraft.getInstance().gui.setScreen(KonfigClientScreens.create(Constants.MOD_ID, Minecraft.getInstance().gui.screen())));
        LiteminerClient.init();
    }
}
