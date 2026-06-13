package com.iamkaf.liteminer.config;

import com.iamkaf.konfig.api.v1.ConfigBuilder;
import com.iamkaf.konfig.api.v1.ConfigValue;

public final class LiteminerConfig {
    public final ConfigValue<Boolean> requireCorrectToolEnabled;
    public final ConfigValue<Boolean> preventToolBreaking;
    public final ConfigValue<Integer> blockBreakLimit;
    public final ConfigValue<Boolean> harvestTimePerBlockModifierEnabled;
    public final ConfigValue<Double> harvestTimePerBlockModifier;
    public final ConfigValue<Boolean> foodExhaustionEnabled;
    public final ConfigValue<Double> foodExhaustion;
    public final ConfigValue<Boolean> distinguishGrownCrops;
    public final ConfigValue<Boolean> matchDeepslateOreVariants;

    public LiteminerConfig(ConfigBuilder builder) {
        builder.push("safety")
                .categoryComment("Safety checks that prevent vein mining from doing something destructive or unintended.")
                .categoryInfo(info -> info
                        .headerKey("liteminer.config.info.safety.header")
                        .inlineTextKey("liteminer.config.info.safety.text"))
                .header("Safety");
        preventToolBreaking = builder.bool("prevent_tool_breaking", true)
                .comment("Stops vein mining before the active tool breaks.")
                .info(info -> info.inlineTextKey("liteminer.config.prevent_tool_breaking.info"))
                .sync(true)
                .build();
        requireCorrectToolEnabled = builder.bool("require_correct_tool_enabled", false)
                .comment("Requires the held tool to be appropriate for every block in the vein.")
                .info(info -> info.inlineTextKey("liteminer.config.require_correct_tool_enabled.info"))
                .sync(true)
                .build();
        builder.pop();

        builder.push("limits")
                .categoryComment("Hard limits for a single vein mining action.")
                .categoryInfo(info -> info
                        .headerKey("liteminer.config.info.limits.header")
                        .inlineTextKey("liteminer.config.info.limits.text"))
                .header("Limits");
        blockBreakLimit = builder.intRange("block_break_limit", 64, 1, 2048)
                .comment("Maximum number of blocks that can be broken by one vein mining action.")
                .info(info -> info.inlineTextKey("liteminer.config.block_break_limit.info"))
                .sync(true)
                .build();
        builder.pop();

        builder.push("harvest")
                .categoryComment("Gameplay costs applied while vein mining.")
                .categoryInfo(info -> info
                        .headerKey("liteminer.config.info.harvest.header")
                        .inlineTextKey("liteminer.config.info.harvest.text"))
                .header("Harvest Cost");
        harvestTimePerBlockModifierEnabled =
                builder.bool("harvest_time_per_block_modifier_enabled", true)
                        .comment("Enables additional mining time based on how many blocks are selected.")
                        .info(info -> info.inlineTextKey("liteminer.config.harvest_time_per_block_modifier_enabled.info"))
                        .sync(true)
                        .build();
        harvestTimePerBlockModifier = builder.doubleRange("harvest_time_per_block_modifier", 2d, 1.0d, 10d)
                .comment("Multiplier used when scaling mining time for larger vein mining selections.")
                .info(info -> info.inlineTextKey("liteminer.config.harvest_time_per_block_modifier.info"))
                .sync(true)
                .build();

        foodExhaustionEnabled = builder.bool("food_exhaustion_enabled", true)
                .comment("Enables hunger exhaustion per block mined by vein mining.")
                .info(info -> info.inlineTextKey("liteminer.config.food_exhaustion_enabled.info"))
                .sync(true)
                .build();
        foodExhaustion = builder.doubleRange("food_exhaustion", 0.2d, 0.0d, 1d)
                .comment("Food exhaustion applied for each block broken by vein mining.")
                .info(info -> info.inlineTextKey("liteminer.config.food_exhaustion.info"))
                .sync(true)
                .build();
        builder.pop();

        builder.push("matching")
                .categoryComment("Rules for deciding which neighboring blocks belong to the same vein.")
                .categoryInfo(info -> info
                        .headerKey("liteminer.config.info.matching.header")
                        .inlineTextKey("liteminer.config.info.matching.text"))
                .header("Block Matching");
        distinguishGrownCrops = builder.bool("distinguish_grown_crops", true)
                .comment("Keeps fully-grown crops and growing crops in separate vein mining groups.")
                .info(info -> info.inlineTextKey("liteminer.config.distinguish_grown_crops.info"))
                .sync(true)
                .build();

        matchDeepslateOreVariants = builder.bool("match_deepslate_ore_variants", true)
                .comment("Treats regular and deepslate variants of the same ore as matching blocks.")
                .info(info -> info.inlineTextKey("liteminer.config.match_deepslate_ore_variants.info"))
                .sync(true)
                .build();
        builder.pop();
    }
}
