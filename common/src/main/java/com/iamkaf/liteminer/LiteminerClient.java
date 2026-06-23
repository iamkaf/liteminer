package com.iamkaf.liteminer;

import com.iamkaf.amber.api.event.v1.events.common.client.ClientCommandEvents;
import com.iamkaf.amber.api.functions.v1.PlayerFunctions;
import com.iamkaf.amber.api.event.v1.events.common.client.ClientTickEvents;
import com.iamkaf.amber.api.event.v1.events.common.client.HudEvents;
import com.iamkaf.amber.api.event.v1.events.common.client.InputEvents;
import com.iamkaf.amber.api.event.v1.events.common.client.RenderEvents;
import com.iamkaf.amber.api.registry.v1.KeybindHelper;
import com.iamkaf.konfig.api.v1.ConfigBuilder;
import com.iamkaf.konfig.api.v1.ConfigHandle;
import com.iamkaf.konfig.api.v1.ConfigMigrationContext;
import com.iamkaf.konfig.api.v1.ConfigScope;
import com.iamkaf.konfig.api.v1.Konfig;
import com.iamkaf.konfig.api.v1.SyncMode;
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
    public static final ConfigHandle CONFIG_HANDLE;
    public static HashSet<BlockPos> selectedBlocks = HashSet.newHashSet(0);
    public static Cycler<LiteminerShape> shapes = new Cycler<>(LiteminerShapes.all());
    private static boolean currentState = false;
    private static long lastChange = System.currentTimeMillis();
    private static Runnable openConfigScreenCallback;
    private static boolean pendingConfigScreenOpen;

    static {
        ConfigBuilder builder = Konfig.builder(Constants.MOD_ID, "client")
                .scope(ConfigScope.CLIENT)
                .syncMode(SyncMode.NONE)
                .fileName("liteminer-client.toml")
                .schemaVersion(1)
                .migrate(0, context -> {
                    context.rename("key_mode", "controls.key_mode");
                    context.rename("show_hud", "hud.show_hud");
                    context.rename("hud_scale", "hud.hud_scale");
                    context.rename("show_highlights", "highlights.show_highlights");
                    context.remove("highlight_color_transition");
                    context.rename("highlight_foreground_line_color", "highlights.highlight_foreground_line_color");
                    context.rename("highlight_see_through_line_color", "highlights.highlight_see_through_line_color");
                    context.remove("highlights.highlight_color_transition");
                    migrateLineColor(context, "highlights.highlight_foreground_line_color", "#F9FFFE");
                    migrateLineColor(context, "highlights.highlight_see_through_line_color", "#169C9C");
                })
                .comment("Client-only Liteminer settings for controls, HUD, and rendering.")
                .info(info -> info
                        .headerKey("liteminer.config.info.client.header")
                        .inlineTextKey("liteminer.config.info.client.text")
                        .urlKey("liteminer.config.info.report_issue", "https://github.com/iamkaf/liteminer"));
        CONFIG = new LiteminerClientConfig(builder);
        CONFIG_HANDLE = builder.build();
    }

    private static void migrateLineColor(ConfigMigrationContext context, String path, String fallback) {
        String value;
        try {
            value = context.string(path);
        } catch (RuntimeException ignored) {
            context.set(path, fallback);
            return;
        }

        if (value == null) {
            return;
        }
        context.set(path, lineColorToHex(value, fallback));
    }

    private static String lineColorToHex(String value, String fallback) {
        String normalized = value.trim();
        if (normalized.startsWith("#") || normalized.regionMatches(true, 0, "0x", 0, 2)) {
            return normalized;
        }

        return switch (normalized.toUpperCase().replace('-', '_').replace(' ', '_')) {
            case "WHITE" -> "#F9FFFE";
            case "ORANGE" -> "#F9801D";
            case "MAGENTA" -> "#C74EBD";
            case "LIGHT_BLUE" -> "#3AB3DA";
            case "YELLOW" -> "#FED83D";
            case "LIME" -> "#80C71F";
            case "PINK" -> "#F38BAA";
            case "GRAY" -> "#474F52";
            case "LIGHT_GRAY" -> "#9D9D97";
            case "CYAN" -> "#169C9C";
            case "PURPLE" -> "#8932B8";
            case "BLUE" -> "#3C44AA";
            case "BROWN" -> "#835432";
            case "GREEN" -> "#5E7C16";
            case "RED" -> "#B02E26";
            case "BLACK" -> "#1D1D21";
            default -> fallback;
        };
    }

    public static void init() {
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

        if ((System.currentTimeMillis() - getLastChange()) < PACKET_DELAY) {
            return;
        }

        switch (CONFIG.keyMode()) {
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
        LiteminerNetwork.sendToServer(new C2SVeinmineKeybindChange(isVeinMining(), shapes.getCurrentIndex()));
        return 1;
    }

    public static void setOpenConfigScreenCallback(Runnable callback) {
        openConfigScreenCallback = callback;
    }
}
