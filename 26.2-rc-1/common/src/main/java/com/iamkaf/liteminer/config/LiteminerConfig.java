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
                .categoryTooltip("Tool and durability safeguards.")
                .categoryInfo(info -> info
                        .header("Safety")
                        .inlineText("These settings decide when vein mining should refuse to continue instead of breaking blocks."));
        preventToolBreaking = builder.bool("prevent_tool_breaking", true)
                .comment("Stops vein mining before the active tool breaks.")
                .tooltip("Stops vein mining when the durability of the tool gets to 1.")
                .info(info -> info.inlineText("Use this to avoid accidentally destroying an enchanted or otherwise valuable tool."))
                .sync(true)
                .build();
        requireCorrectToolEnabled = builder.bool("require_correct_tool_enabled", false)
                .comment("Requires the held tool to be appropriate for every block in the vein.")
                .tooltip("If enabled, blocks can only be broken if holding the correct tool.")
                .info(info -> info.inlineText("This is stricter, but avoids wasting durability or breaking blocks with the wrong harvest tool."))
                .sync(true)
                .build();
        builder.pop();

        builder.push("limits")
                .categoryComment("Hard limits for a single vein mining action.")
                .categoryTooltip("Caps the size of one vein mine operation.")
                .categoryInfo(info -> info
                        .header("Limits")
                        .inlineText("Limits keep very large connected structures from being mined all at once."));
        blockBreakLimit = builder.intRange("block_break_limit", 64, 1, 2048)
                .comment("Maximum number of blocks that can be broken by one vein mining action.")
                .tooltip("Upper limit to how many blocks can be broken at a time.")
                .info(info -> info.inlineText("Higher limits are useful for large ore clusters or tree-like structures, but increase gameplay impact."))
                .sync(true)
                .build();
        builder.pop();

        builder.push("harvest")
                .categoryComment("Gameplay costs applied while vein mining.")
                .categoryTooltip("Break-speed and hunger costs.")
                .categoryInfo(info -> info
                        .header("Harvest Cost")
                        .inlineText("These settings make larger vein mining actions slower or more expensive instead of free."));
        harvestTimePerBlockModifierEnabled =
                builder.bool("harvest_time_per_block_modifier_enabled", true)
                        .comment("Enables additional mining time based on how many blocks are selected.")
                        .tooltip("Increases the time it takes to break a block when vein mining.")
                        .info(info -> info.inlineText("When enabled, larger selections reduce effective break speed before the block breaks."))
                        .sync(true)
                        .build();
        harvestTimePerBlockModifier = builder.doubleRange("harvest_time_per_block_modifier", 2d, 1.0d, 10d)
                .comment("Multiplier used when scaling mining time for larger vein mining selections.")
                .tooltip("Modifier used to scale up the time it needs to mine a block. Higher values mean longer times to mine.")
                .info(info -> info.inlineText("This value only matters when increased harvesting time is enabled."))
                .sync(true)
                .build();

        foodExhaustionEnabled = builder.bool("food_exhaustion_enabled", true)
                .comment("Enables hunger exhaustion per block mined by vein mining.")
                .tooltip("Causes your hunger to go down per block mined.")
                .info(info -> info.inlineText("This makes vein mining cost hunger in proportion to the number of blocks broken."))
                .sync(true)
                .build();
        foodExhaustion = builder.doubleRange("food_exhaustion", 0.2d, 0.0d, 1d)
                .comment("Food exhaustion applied for each block broken by vein mining.")
                .tooltip("How much food exhaustion to cause per block broken.")
                .info(info -> info.inlineText("This value only matters when food exhaustion is enabled."))
                .sync(true)
                .build();
        builder.pop();

        builder.push("matching")
                .categoryComment("Rules for deciding which neighboring blocks belong to the same vein.")
                .categoryTooltip("Block matching behavior.")
                .categoryInfo(info -> info
                        .header("Block Matching")
                        .inlineText("Matching rules decide whether similar blocks should be treated as part of the selected vein."));
        distinguishGrownCrops = builder.bool("distinguish_grown_crops", true)
                .comment("Keeps fully-grown crops and growing crops in separate vein mining groups.")
                .tooltip("If enabled, vein mining crops will only select crops that are grown, or only select crops that are still growing.")
                .info(info -> info.inlineText("Enable this to harvest mature crops without clearing immature crops of the same type."))
                .sync(true)
                .build();

        matchDeepslateOreVariants = builder.bool("match_deepslate_ore_variants", true)
                .comment("Treats regular and deepslate variants of the same ore as matching blocks.")
                .tooltip("If enabled, vein mining will treat <name>_ore and deepslate_<name>_ore as the same block.")
                .info(info -> info.inlineText("Useful around deepslate transition layers where ore variants appear in the same deposit."))
                .sync(true)
                .build();
        builder.pop();
    }
}
