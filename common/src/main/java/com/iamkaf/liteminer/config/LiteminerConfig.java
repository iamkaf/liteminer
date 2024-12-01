package com.iamkaf.liteminer.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class LiteminerConfig {
    public final ForgeConfigSpec.ConfigValue<Boolean> requireCorrectToolEnabled;
    public final ForgeConfigSpec.ConfigValue<Boolean> preventToolBreaking;
    public final ForgeConfigSpec.ConfigValue<Integer> blockBreakLimit;
    public final ForgeConfigSpec.ConfigValue<Boolean> harvestTimePerBlockModifierEnabled;
    public final ForgeConfigSpec.ConfigValue<Double> harvestTimePerBlockModifier;
    public final ForgeConfigSpec.ConfigValue<Boolean> foodExhaustionEnabled;
    public final ForgeConfigSpec.ConfigValue<Double> foodExhaustion;

//    public final ForgeConfigSpec.ConfigValue<Boolean> requireFood;
//    public final ForgeConfigSpec.ConfigValue<Boolean> useToolWhitelist;
//    public final ForgeConfigSpec.ConfigValue<List<ResourceLocation>> toolWhitelist;
//    public final ForgeConfigSpec.ConfigValue<Boolean> useBlockWhitelist;
//    public final ForgeConfigSpec.ConfigValue<List<Block>> blockWhitelist;

    public LiteminerConfig(ForgeConfigSpec.Builder builder) {
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
