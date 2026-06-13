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
                        .header("Safety")
                        .inlineText("These settings decide when vein mining should refuse to continue instead of breaking blocks."))
                .header("Safety");
        preventToolBreaking = builder.bool("prevent_tool_breaking", true)
                .comment("Stops vein mining before the active tool breaks.")
                .info(info -> info.inlineText(
                        "Liteminer leaves the tool's final durability point untouched so valuable tools are not destroyed by a large selection."))
                .sync(true)
                .build();
        requireCorrectToolEnabled = builder.bool("require_correct_tool_enabled", false)
                .comment("Requires the held tool to be appropriate for every block in the vein.")
                .info(info -> info.inlineText(
                        "This stricter check avoids wasting durability or breaking blocks with a tool that is not suited for the selected vein."))
                .sync(true)
                .build();
        builder.pop();

        builder.push("limits")
                .categoryComment("Hard limits for a single vein mining action.")
                .categoryInfo(info -> info
                        .header("Limits")
                        .inlineText("Limits keep very large connected structures from being mined all at once."))
                .header("Limits");
        blockBreakLimit = builder.intRange("block_break_limit", 64, 1, 2048)
                .comment("Maximum number of blocks that can be broken by one vein mining action.")
                .info(info -> info.inlineText(
                        "Higher limits are useful for large ore clusters or tree-like structures, but they make each vein mining action more powerful."))
                .sync(true)
                .build();
        builder.pop();

        builder.push("harvest")
                .categoryComment("Gameplay costs applied while vein mining.")
                .categoryInfo(info -> info
                        .header("Harvest Cost")
                        .inlineText("These settings make larger vein mining actions slower or more expensive instead of free."))
                .header("Harvest Cost");
        harvestTimePerBlockModifierEnabled =
                builder.bool("harvest_time_per_block_modifier_enabled", true)
                        .comment("Enables additional mining time based on how many blocks are selected.")
                        .info(info -> info.inlineText(
                                "When enabled, Liteminer reduces effective break speed before the first block breaks based on the selection size."))
                        .sync(true)
                        .build();
        harvestTimePerBlockModifier = builder.doubleRange("harvest_time_per_block_modifier", 2d, 1.0d, 10d)
                .comment("Multiplier used when scaling mining time for larger vein mining selections.")
                .info(info -> info.inlineText(
                        "Higher values make large selections take longer to start mining. This only matters when harvest time scaling is enabled."))
                .sync(true)
                .build();

        foodExhaustionEnabled = builder.bool("food_exhaustion_enabled", true)
                .comment("Enables hunger exhaustion per block mined by vein mining.")
                .info(info -> info.inlineText(
                        "When enabled, Liteminer applies hunger exhaustion for each block broken by a vein mining action."))
                .sync(true)
                .build();
        foodExhaustion = builder.doubleRange("food_exhaustion", 0.2d, 0.0d, 1d)
                .comment("Food exhaustion applied for each block broken by vein mining.")
                .info(info -> info.inlineText(
                        "Larger values make each mined block consume more hunger. This only matters when food exhaustion is enabled."))
                .sync(true)
                .build();
        builder.pop();

        builder.push("matching")
                .categoryComment("Rules for deciding which neighboring blocks belong to the same vein.")
                .categoryInfo(info -> info
                        .header("Block Matching")
                        .inlineText("Matching rules decide whether similar blocks should be treated as part of the selected vein."))
                .header("Block Matching");
        distinguishGrownCrops = builder.bool("distinguish_grown_crops", true)
                .comment("Keeps fully-grown crops and growing crops in separate vein mining groups.")
                .info(info -> info.inlineText(
                        "Enable this to harvest mature crops without clearing immature crops of the same type."))
                .sync(true)
                .build();

        matchDeepslateOreVariants = builder.bool("match_deepslate_ore_variants", true)
                .comment("Treats regular and deepslate variants of the same ore as matching blocks.")
                .info(info -> info.inlineText(
                        "Useful around deepslate transition layers where regular and deepslate ore variants appear in the same deposit."))
                .sync(true)
                .build();
        builder.pop();
    }
}
