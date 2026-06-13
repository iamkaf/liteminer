package com.iamkaf.liteminer.config;

import com.iamkaf.konfig.api.v1.ConfigBuilder;
import com.iamkaf.konfig.api.v1.ConfigValue;

public final class LiteminerClientConfig {
    public final ConfigValue<KeyMode> keyMode;
    public final ConfigValue<Boolean> showHUD;
    public final ConfigValue<Double> hud_scale;

    public final ConfigValue<Boolean> showHighlights;
    public final ConfigValue<Integer> highlightForegroundLineColor;
    public final ConfigValue<Integer> highlightSeeThroughLineColor;

    public LiteminerClientConfig(ConfigBuilder builder) {
        builder.push("controls")
                .categoryComment("Local input behavior for activating vein mining.")
                .categoryInfo(info -> info
                        .header("Controls")
                        .inlineText("Choose whether the vein mining key must be held down or toggles the mode on and off."))
                .header("Controls");
        keyMode = builder.enumValue("key_mode", KeyMode.HOLD)
                .comment("Controls how the vein mining keybind activates vein mining.")
                .info(info -> info.inlineText(
                        "Hold mode is safer for quick mining. Toggle mode is more comfortable for longer vein mining sessions."))
                .clientOnly()
                .build();
        builder.pop();

        builder.push("hud")
                .categoryComment("Client HUD text shown while vein mining.")
                .categoryInfo(info -> info
                        .header("HUD")
                        .inlineText("HUD settings control the selected block count and current shape text shown in-game."))
                .header("HUD");
        showHUD = builder.bool("show_hud", true)
                .comment("Shows selected block count and current vein mining shape on the HUD.")
                .info(info -> info.inlineText(
                        "Disable this if you prefer to rely on the selected block highlights without extra HUD text."))
                .clientOnly()
                .build();
        hud_scale = builder.doubleRange("hud_scale", 1d, 0.5d, 2d)
                .comment("Scales Liteminer HUD text.")
                .info(info -> info.inlineText(
                        "Values below 1.0 shrink the HUD, while values above 1.0 enlarge it."))
                .clientOnly()
                .build();
        builder.pop();

        builder.push("highlights")
                .categoryComment("Client block outline rendering for selected vein mining blocks.")
                .categoryInfo(info -> info
                        .header("Highlights")
                        .inlineText("Highlights show which blocks will be mined before you break the targeted block."))
                .header("Highlights");
        showHighlights = builder.bool("show_highlights", true)
                .comment("Shows block highlights when vein mining.")
                .info(info -> info.inlineText(
                        "Disable this if the outlines are distracting or too expensive on your client."))
                .clientOnly()
                .build();

        highlightForegroundLineColor = builder.colorRgb("highlight_foreground_line_color", 0xF9FFFE)
                .comment("Color used for normal visible highlight lines.")
                .info(info -> info.inlineText(
                        "These lines render normally and are most visible when the selected block face is unobstructed."))
                .clientOnly()
                .build();

        highlightSeeThroughLineColor = builder.colorRgb("highlight_see_through_line_color", 0x169C9C)
                .comment("Color used for see-through highlight lines.")
                .info(info -> info.inlineText(
                        "These lines render through blocks so selected blocks remain readable behind terrain."))
                .clientOnly()
                .build();
        builder.pop();
    }
}
