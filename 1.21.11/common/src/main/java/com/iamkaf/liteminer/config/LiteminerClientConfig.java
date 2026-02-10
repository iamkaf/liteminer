package com.iamkaf.liteminer.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class LiteminerClientConfig {
    public final ModConfigSpec.ConfigValue<KeyMode> keyMode;
    public final ModConfigSpec.ConfigValue<Boolean> showHUD;
    public final ModConfigSpec.ConfigValue<Double> hud_scale;

    public final ModConfigSpec.ConfigValue<Boolean> showHighlights;
    public final ModConfigSpec.ConfigValue<LineColor> highlightForegroundLineColor;
    public final ModConfigSpec.ConfigValue<LineColor> highlightSeeThroughLineColor;

    public LiteminerClientConfig(ModConfigSpec.Builder builder) {
        keyMode = builder.translation("liteminer.config.key_mode")
                .comment(":)")
                .defineEnum("key_mode", KeyMode.HOLD);
        showHUD = builder.translation("liteminer.config.show_hud").comment(":)").define("show_hud", true);
        hud_scale = builder.translation("liteminer.config.hud_scale")
                .comment(":)")
                .defineInRange("hud_scale", 1d, 0.5d, 2d);

        showHighlights = builder.translation("liteminer.config.show_highlights")
                .comment("Show block highlights when veinmining")
                .define("show_highlights", true);

        highlightForegroundLineColor = builder.translation("liteminer.config.highlight_foreground_line_color")
                .comment(":)")
                .defineEnum("highlight_foreground_line_color", LineColor.WHITE);

        highlightSeeThroughLineColor = builder.translation("liteminer.config.highlight_see_through_line_color")
                .comment(":)")
                .defineEnum("highlight_see_through_line_color", LineColor.CYAN);
    }
}
