package com.iamkaf.liteminer;

import com.iamkaf.liteminer.networking.LiteminerNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class LiteminerClient {
    public static final int PACKET_DELAY = 125;
    public static final KeyMapping KEY_MAPPING = new KeyMapping(
            "key.liteminer.veinmine",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            "key.categories.liteminer"
    );
    private static boolean currentState = false;
    private static long lastChange = System.currentTimeMillis();

    public static void init() {
        KeyMappingRegistry.register(KEY_MAPPING);
        ClientTickEvent.CLIENT_POST.register(LiteminerClient::onPostTick);
    }

    public static void onPostTick(Minecraft minecraft) {
        if ((System.currentTimeMillis() - getLastChange()) < PACKET_DELAY) {
            return;
        }

        switch (Liteminer.CONFIG.keyMode.get()) {
            case HOLD -> {
                var newState = KEY_MAPPING.isDown();

                if (newState == isVeinMining()) {
                    return;
                }

                new LiteminerNetwork.Messages.C2SVeinmineKeybindChange(newState).sendToServer();
                currentState = newState;
            }
            case TOGGLE -> {
                if (KEY_MAPPING.consumeClick()) {
                    var newState = !isVeinMining();
                    new LiteminerNetwork.Messages.C2SVeinmineKeybindChange(newState).sendToServer();
                    currentState = newState;
                }
            }
        }


    }

    public static boolean isVeinMining() {
        return currentState;
    }

    public static long getLastChange() {
        return lastChange;
    }

    public static void setLastChange(long lastChange) {
        LiteminerClient.lastChange = lastChange;
    }
}
