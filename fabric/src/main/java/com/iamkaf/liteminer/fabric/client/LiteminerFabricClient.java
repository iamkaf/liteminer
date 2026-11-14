package com.iamkaf.liteminer.fabric.client;

import com.iamkaf.liteminer.LiteminerClient;
import net.fabricmc.api.ClientModInitializer;

public final class LiteminerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LiteminerClient.init();
    }
}
