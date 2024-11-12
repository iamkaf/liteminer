package com.iamkaf.liteminer;

import com.iamkaf.liteminer.config.LiteminerConfig;
import com.iamkaf.liteminer.event.Events;
import com.iamkaf.liteminer.networking.LiteminerNetwork;
import com.iamkaf.liteminer.walker.Walker;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Liteminer {
    public static final String MOD_ID = "liteminer";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final LiteminerConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;
    public static Liteminer instance;

    static {
        Pair<LiteminerConfig, ModConfigSpec> pair =
                new ModConfigSpec.Builder().configure(LiteminerConfig::new);
        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
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
    public static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static float getScaledBreakSpeedModifier(int blockCount) {
        float modifier = CONFIG.harvestTimePerBlockModifier.get().floatValue();
        float blockBreakLimit = CONFIG.blockBreakLimit.get().floatValue();

        return 1 - blockCount / blockBreakLimit * modifier * 0.1f * 0.95f;
    }

    public LiteminerPlayerState getPlayerState(ServerPlayer player) {
        return playerStateMap.computeIfAbsent(player.getUUID(), LiteminerPlayerState::new);
    }

    public void onKeymappingStateChange(ServerPlayer player, boolean keybindState) {
        var playerState = getPlayerState(player);
        playerState.setKeymappingState(keybindState);
    }

    public float onBreakSpeed(ServerPlayer player, float originalSpeed) {
        var isVeinMining = getPlayerState(player).getKeymappingState();

        if (isVeinMining) {
            Walker walker = new Walker();
            Level level = player.level();
            // TODO: optimize this by caching the block list
            int blockCount = walker.walk(level, player, Walker.raytrace(level, player).getBlockPos()).size();
            return getScaledBreakSpeedModifier(blockCount);
        }
        return 1f;
    }
}
