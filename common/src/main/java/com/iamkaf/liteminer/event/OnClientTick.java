package com.iamkaf.liteminer.event;

import com.iamkaf.liteminer.LiteminerClient;
import com.iamkaf.liteminer.networking.LiteminerNetwork;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;

public class OnClientTick {
    public static final int PACKET_DELAY = 125;
    public static boolean currentState = false;
    public static long lastChange = System.currentTimeMillis();

    private static void onPostTick(Minecraft minecraft) {
        if ((System.currentTimeMillis() - lastChange) < PACKET_DELAY) {
            return;
        }

        var newState = LiteminerClient.KEY_MAPPING.isDown();

        if (newState == currentState) {
            return;
        }

        assert minecraft.player != null;
        new LiteminerNetwork.Messages.C2SVeinmineKeybindChange(newState).sendToServer();
        currentState = newState;
    }

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(OnClientTick::onPostTick);
    }
}