package com.iamkaf.liteminer.fabric.client;

import com.iamkaf.konfig.api.v1.KonfigClientScreens;
import com.iamkaf.liteminer.Constants;
import com.iamkaf.liteminer.LiteminerClient;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

public final class LiteminerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LiteminerClient.init();
        LiteminerClient.setOpenConfigScreenCallback(
                () -> Minecraft.getInstance().gui.setScreen(KonfigClientScreens.create(Constants.MOD_ID, Minecraft.getInstance().gui.screen())));
    }
}
