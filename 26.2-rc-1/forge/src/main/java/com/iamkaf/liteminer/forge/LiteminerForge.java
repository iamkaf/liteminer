package com.iamkaf.liteminer.forge;

import com.iamkaf.liteminer.Constants;
import com.iamkaf.liteminer.LiteminerClient;
import com.iamkaf.liteminer.LiteminerMod;
import com.iamkaf.amber.api.platform.v1.Platform;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Constants.MOD_ID)
public class LiteminerForge {

    public LiteminerForge() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            LiteminerClient.setOpenConfigScreenCallback(
                    () -> LiteminerClient.showConfigScreenUnavailableMessage(Platform.getConfigFolder()));
            LiteminerClient.init();
        }
        LiteminerMod.init();
    }
}
