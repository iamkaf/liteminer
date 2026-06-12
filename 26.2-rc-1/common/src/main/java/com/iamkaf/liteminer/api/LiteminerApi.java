package com.iamkaf.liteminer.api;

import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.LiteminerPlayerState;
import com.iamkaf.liteminer.api.shape.LiteminerShape;
import com.iamkaf.liteminer.api.shape.LiteminerShapes;
import com.iamkaf.liteminer.networking.LiteminerNetwork;
import com.iamkaf.liteminer.networking.S2CSetShape;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Stable server-side helpers for reading and changing Liteminer player state.
 */
public final class LiteminerApi {
    private LiteminerApi() {
    }

    /**
     * Returns whether the player currently has Liteminer's veinmine input enabled on the server.
     *
     * @param player the player to inspect
     * @return {@code true} when Liteminer will process compatible break/interact events for this player
     */
    public static boolean isVeinmining(ServerPlayer player) {
        return state(player).getKeymappingState();
    }

    /**
     * Returns the player's currently selected shape index.
     *
     * @param player the player to inspect
     * @return the selected shape index in {@link LiteminerShapes#all()}
     */
    public static int getSelectedShapeIndex(ServerPlayer player) {
        return state(player).getShape();
    }

    /**
     * Returns the player's currently selected shape.
     *
     * @param player the player to inspect
     * @return the selected shape, or {@link Optional#empty()} if no shapes are registered
     */
    public static Optional<LiteminerShape> getSelectedShape(ServerPlayer player) {
        return LiteminerShapes.byIndex(getSelectedShapeIndex(player));
    }

    /**
     * Changes the player's selected shape by shape id and synchronizes the client HUD state.
     *
     * @param player  the player to update
     * @param shapeId the id of a shape registered in {@link LiteminerShapes}
     * @return {@code true} if the shape exists and was selected, otherwise {@code false}
     */
    public static boolean setSelectedShape(ServerPlayer player, Identifier shapeId) {
        int index = LiteminerShapes.indexOf(shapeId);
        if (index < 0) {
            return false;
        }

        state(player).setShape(index);
        LiteminerNetwork.sendToPlayer(new S2CSetShape(index), player);
        return true;
    }

    /**
     * Returns the configured maximum number of blocks Liteminer may process in one operation.
     *
     * @return the current block break limit from Liteminer's common config
     */
    public static int getBlockLimit() {
        return Liteminer.CONFIG.blockBreakLimit.get();
    }

    private static LiteminerPlayerState state(ServerPlayer player) {
        return Liteminer.instance.getPlayerState(player);
    }
}
