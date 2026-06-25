package com.iamkaf.liteminer;

import com.iamkaf.liteminer.config.LiteminerConfig;
import com.iamkaf.liteminer.event.Events;
import com.iamkaf.liteminer.networking.LiteminerNetwork;
import com.iamkaf.liteminer.api.shape.LiteminerShapes;
import com.iamkaf.liteminer.shapes.ShapelessWalker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Liteminer {
    public static final String MOD_ID = Constants.MOD_ID;
    public static final Logger LOGGER = Constants.LOG;
    public static final LiteminerConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;
    public static Liteminer instance;

    static {
        Pair<LiteminerConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(LiteminerConfig::new);
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
