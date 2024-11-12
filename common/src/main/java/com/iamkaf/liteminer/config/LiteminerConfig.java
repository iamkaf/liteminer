package com.iamkaf.liteminer.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class LiteminerConfig {
    public final ModConfigSpec.ConfigValue<KeyMode> keyMode;
    public final ModConfigSpec.ConfigValue<Boolean> preventToolBreaking;
    public final ModConfigSpec.ConfigValue<Integer> blockBreakLimit;
    public final ModConfigSpec.ConfigValue<Boolean> harvestTimePerBlockModifierEnabled;
    public final ModConfigSpec.ConfigValue<Double> harvestTimePerBlockModifier;
    public final ModConfigSpec.ConfigValue<Boolean> foodExhaustionEnabled;
    public final ModConfigSpec.ConfigValue<Double> foodExhaustion;

    public LiteminerConfig(ModConfigSpec.Builder builder) {
        keyMode = builder.translation("liteminer.config.key_mode").defineEnum("key_mode", KeyMode.HOLD);

        preventToolBreaking = builder.translation("liteminer.config.prevent_tool_breaking")
                .define("prevent_tool_breaking", true);

        blockBreakLimit = builder.translation("liteminer.config.block_break_limit")
                .defineInRange("block_break_limit", 64, 1, 2048);

        harvestTimePerBlockModifierEnabled =
                builder.translation("liteminer.config.harvest_time_per_block_modifier_enabled")
                        .define("harvest_time_per_block_modifier_enabled", true);
        harvestTimePerBlockModifier = builder.translation("liteminer.config.harvest_time_per_block_modifier")
                .defineInRange("harvest_time_per_block_modifier", 2d, 1.0d, 10d);

        foodExhaustionEnabled = builder.translation("liteminer.config.food_exhaustion_enabled")
                .define("food_exhaustion_enabled", true);
        foodExhaustion = builder.translation("liteminer.config.food_exhaustion")
                .defineInRange("food_exhaustion", 0.2d, 0.0d, 1d);
    }

    public enum KeyMode {
        HOLD,
        TOGGLE
    }
}