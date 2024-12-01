package com.iamkaf.liteminer.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class LiteminerConfig {
    public final ModConfigSpec.ConfigValue<Boolean> requireCorrectToolEnabled;
    public final ModConfigSpec.ConfigValue<Boolean> preventToolBreaking;
    public final ModConfigSpec.ConfigValue<Integer> blockBreakLimit;
    public final ModConfigSpec.ConfigValue<Boolean> harvestTimePerBlockModifierEnabled;
    public final ModConfigSpec.ConfigValue<Double> harvestTimePerBlockModifier;
    public final ModConfigSpec.ConfigValue<Boolean> foodExhaustionEnabled;
    public final ModConfigSpec.ConfigValue<Double> foodExhaustion;

//    public final ModConfigSpec.ConfigValue<Boolean> requireFood;
//    public final ModConfigSpec.ConfigValue<Boolean> useToolWhitelist;
//    public final ModConfigSpec.ConfigValue<List<ResourceLocation>> toolWhitelist;
//    public final ModConfigSpec.ConfigValue<Boolean> useBlockWhitelist;
//    public final ModConfigSpec.ConfigValue<List<Block>> blockWhitelist;

    public LiteminerConfig(ModConfigSpec.Builder builder) {
        preventToolBreaking = builder.translation("liteminer.config.prevent_tool_breaking")
                .comment(":)")
                .define("prevent_tool_breaking", true);
        requireCorrectToolEnabled = builder.translation("liteminer.config.require_correct_tool_enabled")
                .comment(":)")
                .define("require_correct_tool_enabled", false);

        blockBreakLimit = builder.translation("liteminer.config.block_break_limit")
                .comment(":)")
                .defineInRange("block_break_limit", 64, 1, 2048);

        harvestTimePerBlockModifierEnabled =
                builder.translation("liteminer.config.harvest_time_per_block_modifier_enabled")
                        .comment(":)")
                        .define("harvest_time_per_block_modifier_enabled", true);
        harvestTimePerBlockModifier = builder.translation("liteminer.config.harvest_time_per_block_modifier")
                .comment(":)")
                .defineInRange("harvest_time_per_block_modifier", 2d, 1.0d, 10d);

        foodExhaustionEnabled = builder.translation("liteminer.config.food_exhaustion_enabled")
                .comment(":)")
                .define("food_exhaustion_enabled", true);
        foodExhaustion = builder.translation("liteminer.config.food_exhaustion")
                .comment(":)")
                .defineInRange("food_exhaustion", 0.2d, 0.0d, 1d);
    }
}
