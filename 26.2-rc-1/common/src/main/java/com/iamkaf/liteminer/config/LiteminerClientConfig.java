package com.iamkaf.liteminer.config;

import com.iamkaf.konfig.api.v1.ConfigBuilder;
import com.iamkaf.konfig.api.v1.ConfigValue;

public final class LiteminerClientConfig {
    public final ConfigValue<KeyMode> keyMode;
    public final ConfigValue<Boolean> showHUD;
    public final ConfigValue<Double> hud_scale;

    public final ConfigValue<Boolean> showHighlights;
    public final ConfigValue<Boolean> highlightColorTransition;
    public final ConfigValue<LineColor> highlightForegroundLineColor;
    public final ConfigValue<LineColor> highlightSeeThroughLineColor;

    public LiteminerClientConfig(ConfigBuilder builder) {
        builder.push("controls")
                .categoryComment("Local input behavior for activating vein mining.")
                .categoryTooltip("Keybind behavior.")
                .categoryInfo(info -> info
                        .header("Controls")
                        .inlineText("Choose whether the vein mining key must be held down or toggles the mode on and off."));
        keyMode = builder.enumValue("key_mode", KeyMode.HOLD)
                .comment("Controls how the vein mining keybind activates vein mining.")
                .tooltip("In Hold mode you must keep holding the key to mine. In toggle mode you hit the key to toggle.")
                .info(info -> info.inlineText("Hold mode is safer for quick mining. Toggle mode is more comfortable for longer sessions."))
                .clientOnly()
                .build();
        builder.pop();

        builder.push("hud")
                .categoryComment("Client HUD text shown while vein mining.")
                .categoryTooltip("On-screen selected block and shape text.")
                .categoryInfo(info -> info
                        .header("HUD")
                        .inlineText("HUD settings control the selected block count and current shape text shown in-game."));
        showHUD = builder.bool("show_hud", true)
                .comment("Shows selected block count and current vein mining shape on the HUD.")
                .tooltip("Enables the text showing how many blocks you have selected and your mining shape.")
                .info(info -> info.inlineText("Disable this if you prefer the visual highlight only."))
                .clientOnly()
                .build();
        hud_scale = builder.doubleRange("hud_scale", 1d, 0.5d, 2d)
                .comment("Scales Liteminer HUD text.")
                .tooltip("Changes how big the HUD elements are.")
                .info(info -> info.inlineText("Values below 1.0 shrink the HUD, while values above 1.0 enlarge it."))
                .clientOnly()
                .build();
        builder.pop();

        builder.push("highlights")
                .categoryComment("Client block outline rendering for selected vein mining blocks.")
                .categoryTooltip("Selected block highlight rendering.")
                .categoryInfo(info -> info
                        .header("Highlights")
                        .inlineText("Highlights show which blocks will be mined before you break the targeted block."));
        showHighlights = builder.bool("show_highlights", true)
                .comment("Shows block highlights when vein mining.")
                .tooltip("Show visual highlights around blocks that will be mined.")
                .info(info -> info.inlineText("Disable this if the outlines are distracting or too expensive on your client."))
                .clientOnly()
                .build();

        highlightColorTransition = builder.bool("highlight_color_transition", false)
                .comment("Animates highlight colors toward their opposite RGB color.")
                .tooltip("Transitions highlight lines toward the opposite RGB color.")
                .info(info -> info.inlineText("This is a visual-only effect. It does not change which blocks are selected."))
                .clientOnly()
                .build();

        highlightForegroundLineColor = builder.enumValue("highlight_foreground_line_color", LineColor.WHITE)
                .comment("Color used for normal visible highlight lines.")
                .tooltip("Color for the main highlight lines using Minecraft wool colors.")
                .info(info -> info.inlineText("These lines render normally and are most visible when the selected block face is unobstructed."))
                .clientOnly()
                .build();

        highlightSeeThroughLineColor = builder.enumValue("highlight_see_through_line_color", LineColor.CYAN)
                .comment("Color used for see-through highlight lines.")
                .tooltip("Color for the see-through highlight lines using Minecraft wool colors.")
                .info(info -> info.inlineText("These lines render through blocks so selected blocks remain readable behind terrain."))
                .clientOnly()
                .build();
        builder.pop();
    }
}
