package com.iamkaf.liteminer.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class LiteminerClientConfig {
    public final ModConfigSpec.ConfigValue<KeyMode> keyMode;
    public final ModConfigSpec.ConfigValue<Boolean> showHUD;
    public final ModConfigSpec.ConfigValue<Double> hud_scale;

    public LiteminerClientConfig(ModConfigSpec.Builder builder) {
        keyMode = builder.translation("liteminer.config.key_mode")
                .comment(":)")
                .defineEnum("key_mode", KeyMode.HOLD);
        showHUD = builder.translation("liteminer.config.show_hud").comment(":)").define("show_hud", true);
        hud_scale = builder.translation("liteminer.config.hud_scale")
                .comment(":)")
                .defineInRange("hud_scale", 1d, 0.5d, 2d);
    }
}
