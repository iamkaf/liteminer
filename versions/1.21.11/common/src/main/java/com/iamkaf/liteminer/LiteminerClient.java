package com.iamkaf.liteminer;

import com.iamkaf.amber.api.event.v1.events.common.client.ClientCommandEvents;
import com.iamkaf.amber.api.functions.v1.PlayerFunctions;
import com.iamkaf.amber.api.event.v1.events.common.client.ClientTickEvents;
import com.iamkaf.amber.api.event.v1.events.common.client.HudEvents;
import com.iamkaf.amber.api.event.v1.events.common.client.InputEvents;
import com.iamkaf.amber.api.event.v1.events.common.client.RenderEvents;
import com.iamkaf.amber.api.registry.v1.KeybindHelper;
import com.iamkaf.liteminer.api.shape.LiteminerShape;
import com.iamkaf.liteminer.api.shape.LiteminerShapes;
import com.iamkaf.liteminer.config.LiteminerClientConfig;
import com.iamkaf.liteminer.networking.C2SVeinmineKeybindChange;
import com.iamkaf.liteminer.networking.LiteminerNetwork;
import com.iamkaf.liteminer.rendering.BlockHighlightRenderer;
import com.iamkaf.liteminer.rendering.HUD;
import com.iamkaf.liteminer.shapes.Cycler;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.HashSet;

public class LiteminerClient {
    public static final int PACKET_DELAY = 125;
    public static final KeyMapping.Category KEY_CATEGORY =
            KeyMapping.Category.register(Constants.resource(Constants.MOD_ID));
    public static final KeyMapping KEY_MAPPING =
            new KeyMapping("key.liteminer.veinmine", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT, KEY_CATEGORY);
    public static final LiteminerClientConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;
    public static HashSet<BlockPos> selectedBlocks = HashSet.newHashSet(0);
    public static Cycler<LiteminerShape> shapes = new Cycler<>(LiteminerShapes.all());
    private static boolean currentState = false;
    private static long lastChange = System.currentTimeMillis();
    private static Runnable openConfigScreenCallback;
    private static boolean pendingConfigScreenOpen;

    static {
        Pair<LiteminerClientConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(LiteminerClientConfig::new);
        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

    public static void init() {
        com.iamkaf.liteminer.networking.ClientLiteminerDoctor.initialize();
        KeybindHelper.register(KEY_MAPPING);
        ClientTickEvents.END_CLIENT_TICK.register(LiteminerClient::onPostTick);
        HudEvents.RENDER_HUD.register(HUD::onRenderHUD);
        InputEvents.MOUSE_SCROLL_PRE.register(HUD::onMouseScroll);
        RenderEvents.BLOCK_OUTLINE_RENDER.register(BlockHighlightRenderer::renderLiteminerHighlight);
        ClientCommandEvents.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(Commands.literal("liteminer")
                        .executes(context -> openConfigCommand())
                        .then(Commands.literal("config")
                                .executes(context -> openConfigCommand()))
                        .then(Commands.literal("shape")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                                .executes(context -> setShapeCommand(
                                                        IntegerArgumentType.getInteger(context, "index")
                                                )))))));
    }

    public static void onPostTick() {
        openPendingConfigScreen();
        com.iamkaf.liteminer.networking.ClientHandshake.tick();
        if (Minecraft.getInstance().player == null) return;

        if ((System.currentTimeMillis() - getLastChange()) < PACKET_DELAY) {
            return;
        }

        switch (CONFIG.keyMode.get()) {
            case HOLD -> {
                var newState = KEY_MAPPING.isDown();

                if (newState == currentState) {
                    return;
                }

                LiteminerNetwork.sendToServer(new C2SVeinmineKeybindChange(newState, shapes.getCurrentIndex()));
                currentState = newState;
            }
            case TOGGLE -> {
                if (KEY_MAPPING.consumeClick()) {
                    var newState = !currentState;

                    LiteminerNetwork.sendToServer(new C2SVeinmineKeybindChange(newState, shapes.getCurrentIndex()));
                    currentState = newState;
                }
            }
        }
    }

    public static void resetInput() {
        currentState = false;
        selectedBlocks.clear();
    }

    /** Whether the player has enabled the local selection preview. */
    public static boolean isPreviewActive() {
        return currentState;
    }

    public static boolean isVeinMining() {
        return currentState && com.iamkaf.liteminer.networking.ClientHandshake.allowsMining();
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

    public static Runnable getOpenConfigScreenCallback() {
        return openConfigScreenCallback;
    }

    public static void openConfigScreen() {
        pendingConfigScreenOpen = true;
    }

    private static void openPendingConfigScreen() {
        if (!pendingConfigScreenOpen) {
            return;
        }

        pendingConfigScreenOpen = false;
        if (openConfigScreenCallback != null) {
            openConfigScreenCallback.run();
            return;
        }

        showConfigScreenUnavailableMessage(null);
    }

    public static void showConfigScreenUnavailableMessage(Path configDirectory) {
        if (Minecraft.getInstance().player != null) {
            Component message = Component.literal("Liteminer's config screen is not available on this loader.");
            if (configDirectory != null) {
                message = Component.empty()
                        .append(message)
                        .append(Component.literal(" "))
                        .append(Component.literal("[Open config folder]").withStyle(style -> style
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent.OpenFile(configDirectory))
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal(configDirectory.toString())))));
            }

            PlayerFunctions.sendMessage(Minecraft.getInstance().player, message);
        }
    }

    private static int openConfigCommand() {
        openConfigScreen();
        return 1;
    }

    private static int setShapeCommand(int index) {
        shapes.setCurrentIndex(index);
        LiteminerNetwork.sendToServer(new C2SVeinmineKeybindChange(currentState, shapes.getCurrentIndex()));
        return 1;
    }

    public static void setOpenConfigScreenCallback(Runnable callback) {
        openConfigScreenCallback = callback;
    }
}
