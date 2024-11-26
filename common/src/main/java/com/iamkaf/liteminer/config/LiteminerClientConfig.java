package com.iamkaf.liteminer.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class LiteminerClientConfig {
    public final ModConfigSpec.ConfigValue<KeyMode> keyMode;
    public final ModConfigSpec.ConfigValue<Boolean> showHUD;

    public LiteminerClientConfig(ModConfigSpec.Builder builder) {
        keyMode = builder.translation("liteminer.config.key_mode").defineEnum("key_mode", KeyMode.HOLD);
        showHUD = builder.translation("liteminer.config.show_hud").define("show_hud", true);
    }
}
