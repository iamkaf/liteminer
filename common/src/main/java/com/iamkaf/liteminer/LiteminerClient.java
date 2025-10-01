package com.iamkaf.liteminer;

import com.iamkaf.amber.api.event.v1.events.common.client.ClientTickEvents;
import com.iamkaf.amber.api.event.v1.events.common.client.HudEvents;
import com.iamkaf.amber.api.event.v1.events.common.client.InputEvents;
import com.iamkaf.amber.api.event.v1.events.common.client.RenderEvents;
import com.iamkaf.amber.api.keymapping.KeybindHelper;
import com.iamkaf.liteminer.config.LiteminerClientConfig;
import com.iamkaf.liteminer.networking.C2SVeinmineKeybindChange;
import com.iamkaf.liteminer.networking.LiteminerNetwork;
import com.iamkaf.liteminer.rendering.BlockHighlightRenderer;
import com.iamkaf.liteminer.rendering.HUD;
import com.iamkaf.liteminer.shapes.Cycler;
import com.iamkaf.liteminer.shapes.Walker;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;

public class LiteminerClient {
    public static final int PACKET_DELAY = 125;
    public static final KeyMapping.Category KEY_CATEGORY =
            KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath("liteminer", "liteminer"));
    public static final KeyMapping KEY_MAPPING =
            new KeyMapping("key.liteminer.veinmine", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT, KEY_CATEGORY);
    public static final LiteminerClientConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;
    public static HashSet<BlockPos> selectedBlocks = HashSet.newHashSet(0);
    public static Cycler<Walker> shapes = new Cycler<>(Liteminer.WALKERS);
    private static boolean currentState = false;
    private static long lastChange = System.currentTimeMillis();

    static {
        Pair<LiteminerClientConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(LiteminerClientConfig::new);
        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

    public static void init() {
        KeybindHelper.register(KEY_MAPPING);
        ClientTickEvents.END_CLIENT_TICK.register(LiteminerClient::onPostTick);
        HudEvents.RENDER_HUD.register(HUD::onRenderHUD);
        InputEvents.MOUSE_SCROLL_PRE.register(HUD::onMouseScroll);
        RenderEvents.BLOCK_OUTLINE_RENDER.register(BlockHighlightRenderer::renderLiteminerHighlight);
    }

    public static void onPostTick() {
        if ((System.currentTimeMillis() - getLastChange()) < PACKET_DELAY) {
            return;
        }

        switch (CONFIG.keyMode.get()) {
            case HOLD -> {
                var newState = KEY_MAPPING.isDown();

                if (newState == isVeinMining()) {
                    return;
                }

                LiteminerNetwork.sendToServer(new C2SVeinmineKeybindChange(newState, shapes.getCurrentIndex()));
                currentState = newState;
            }
            case TOGGLE -> {
                if (KEY_MAPPING.consumeClick()) {
                    var newState = !isVeinMining();

                    LiteminerNetwork.sendToServer(new C2SVeinmineKeybindChange(newState, shapes.getCurrentIndex()));
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
        HitResult result = Minecraft.getInstance().hitResult;
        return result != null && result.getType() == HitResult.Type.BLOCK;
    }
}
