package com.iamkaf.liteminer;

import com.iamkaf.liteminer.config.LiteminerClientConfig;
import com.iamkaf.liteminer.networking.LiteminerNetwork;
import com.iamkaf.liteminer.rendering.HUD;
import com.iamkaf.liteminer.shapes.Cycler;
import com.iamkaf.liteminer.shapes.Walker;
import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;

public class LiteminerClient {
    public static final int PACKET_DELAY = 125;
    public static final KeyMapping KEY_MAPPING = new KeyMapping("key.liteminer.veinmine",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            "key.categories.liteminer"
    );
    public static final LiteminerClientConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;
    public static HashSet<BlockPos> selectedBlocks = HashSet.newHashSet(0);
    public static Minecraft mc;
    public static Cycler<Walker> shapes = new Cycler<>(Liteminer.WALKERS);
    private static boolean currentState = false;
    private static long lastChange = System.currentTimeMillis();

    static {
        Pair<LiteminerClientConfig, ModConfigSpec> pair =
                new ModConfigSpec.Builder().configure(LiteminerClientConfig::new);
        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

    public static void init() {
        mc = Minecraft.getInstance();
        KeyMappingRegistry.register(KEY_MAPPING);
        ClientTickEvent.CLIENT_POST.register(LiteminerClient::onPostTick);
        ClientGuiEvent.RENDER_HUD.register(HUD::onRenderHUD);
        ClientRawInputEvent.MOUSE_SCROLLED.register(HUD::onMouseScroll);
    }

    public static void onPostTick(Minecraft minecraft) {
        if ((System.currentTimeMillis() - getLastChange()) < PACKET_DELAY) {
            return;
        }

        switch (CONFIG.keyMode.get()) {
            case HOLD -> {
                var newState = KEY_MAPPING.isDown();

                if (newState == isVeinMining()) {
                    return;
                }

                new LiteminerNetwork.Messages.C2SVeinmineKeybindChange(newState,
                        shapes.getCurrentIndex()
                ).sendToServer();
                currentState = newState;
            }
            case TOGGLE -> {
                if (KEY_MAPPING.consumeClick()) {
                    var newState = !isVeinMining();
                    new LiteminerNetwork.Messages.C2SVeinmineKeybindChange(newState,
                            shapes.getCurrentIndex()
                    ).sendToServer();
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

    public static boolean isTargetingABlock() {
        HitResult result = mc.hitResult;
        return result != null && result.getType() == HitResult.Type.BLOCK;
    }
}
