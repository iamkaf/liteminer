package com.iamkaf.liteminer;

import com.iamkaf.liteminer.config.LiteminerConfig;
import com.iamkaf.liteminer.event.Events;
import com.iamkaf.liteminer.networking.LiteminerNetwork;
import com.iamkaf.liteminer.api.shape.LiteminerShapes;
import com.iamkaf.liteminer.shapes.ShapelessWalker;
import com.iamkaf.konfig.api.v1.ConfigBuilder;
import com.iamkaf.konfig.api.v1.ConfigHandle;
import com.iamkaf.konfig.api.v1.ConfigScope;
import com.iamkaf.konfig.api.v1.Konfig;
import com.iamkaf.konfig.api.v1.SyncMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Liteminer {
    public static final String MOD_ID = Constants.MOD_ID;
    public static final Logger LOGGER = Constants.LOG;
    public static final LiteminerConfig CONFIG;
    public static final ConfigHandle CONFIG_HANDLE;
    public static Liteminer instance;

    static {
        ConfigBuilder builder = Konfig.builder(MOD_ID, "common")
                .scope(ConfigScope.COMMON)
                .syncMode(SyncMode.LOGIN_AND_RELOAD)
                .fileName("liteminer-common.toml")
                .schemaVersion(1)
                .migrate(0, context -> {
                    context.rename("prevent_tool_breaking", "safety.prevent_tool_breaking");
                    context.rename("require_correct_tool_enabled", "safety.require_correct_tool_enabled");
                    context.rename("block_break_limit", "limits.block_break_limit");
                    context.rename("harvest_time_per_block_modifier_enabled", "harvest.harvest_time_per_block_modifier_enabled");
                    context.rename("harvest_time_per_block_modifier", "harvest.harvest_time_per_block_modifier");
                    context.rename("food_exhaustion_enabled", "harvest.food_exhaustion_enabled");
                    context.rename("food_exhaustion", "harvest.food_exhaustion");
                    context.rename("distinguish_grown_crops", "matching.distinguish_grown_crops");
                    context.rename("match_deepslate_ore_variants", "matching.match_deepslate_ore_variants");
                })
                .comment("Common Liteminer settings synced from the server when multiplayer policy requires it.")
                .info(info -> info
                        .header("Liteminer")
                        .inlineText("Gameplay settings for vein mining limits, safety checks, harvest cost, and block matching.")
                        .url("Report an issue", "https://github.com/iamkaf/mod-issues"));
        CONFIG = new LiteminerConfig(builder);
        CONFIG_HANDLE = builder.build();
    }

    public Map<UUID, LiteminerPlayerState> playerStateMap = new HashMap<>();

    public Liteminer() {
        Liteminer.instance = this;
    }

    public static void init() {
        LOGGER.info("Litemining, from poppies to deepslate.");

        LiteminerNetwork.init();
        Events.init();
    }

    /**
     * Creates resource location in the mod namespace with the given path.
     */
    public static net.minecraft.resources.Identifier resource(String path) {
        return Constants.resource(path);
    }

    public static float getScaledBreakSpeedModifier(int blockCount) {
        if (blockCount <= 1) {
            return 1f;
        }

        float modifier = CONFIG.harvestTimePerBlockModifier.get().floatValue();
        float extraBlockPenalty = (blockCount - 1) * modifier * 0.1f;

        return 1f / (1f + extraBlockPenalty);
    }

    public static int getSelectedBlockCount(Level level, Player player, int shapeIndex) {
        BlockPos origin = ShapelessWalker.raytrace(level, player).getBlockPos();
        return LiteminerShapes.byIndex(shapeIndex)
                .orElseThrow()
                .walk(level, player, origin)
                .size();
    }

    public LiteminerPlayerState getPlayerState(ServerPlayer player) {
        return playerStateMap.computeIfAbsent(player.getUUID(), LiteminerPlayerState::new);
    }

    public void onKeymappingStateChange(ServerPlayer player, boolean keybindState, int shape) {
        var playerState = getPlayerState(player);
        playerState.setKeymappingState(keybindState);
        playerState.setShape(shape);
    }

    public float onBreakSpeed(ServerPlayer player) {
        LiteminerPlayerState playerState = getPlayerState(player);
        var isVeinMining = playerState.getKeymappingState();

        if (isVeinMining) {
            return getScaledBreakSpeedModifier(getSelectedBlockCount(player.level(), player, playerState.getShape()));
        }
        return 1f;
    }
}
