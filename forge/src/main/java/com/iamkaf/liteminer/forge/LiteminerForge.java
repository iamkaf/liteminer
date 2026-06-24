package com.iamkaf.liteminer.forge;

import com.iamkaf.konfig.api.v1.KonfigClientScreens;
import com.iamkaf.konfig.forge.api.v1.KonfigForgeClientScreens;
import com.iamkaf.liteminer.Constants;
import com.iamkaf.liteminer.LiteminerClient;
import com.iamkaf.liteminer.LiteminerMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Constants.MOD_ID)
public class LiteminerForge {

    public LiteminerForge() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            KonfigForgeClientScreens.register(Constants.MOD_ID);
            LiteminerClient.setOpenConfigScreenCallback(
                    () -> Minecraft.getInstance().gui.setScreen(KonfigClientScreens.create(Constants.MOD_ID, Minecraft.getInstance().gui.screen())));
            LiteminerClient.init();
        }
        LiteminerMod.init();
    }
}
