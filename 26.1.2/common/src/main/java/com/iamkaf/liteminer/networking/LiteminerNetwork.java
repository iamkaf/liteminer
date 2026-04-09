package com.iamkaf.liteminer.networking;

import com.iamkaf.amber.api.networking.v1.NetworkChannel;
import com.iamkaf.amber.api.networking.v1.Packet;
import com.iamkaf.liteminer.Liteminer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class LiteminerNetwork {
    public static final NetworkChannel NET =
            NetworkChannel.create(Identifier.fromNamespaceAndPath(Liteminer.MOD_ID, "main"));

    private static final boolean initialized = false;

    public static void init() {
        if (initialized) {
            Liteminer.LOGGER.debug("Liteminer network already initialized");
            return;
        }

        // Register client to server keybind state change packet
        NET.register(
                C2SVeinmineKeybindChange.class,
                C2SVeinmineKeybindChange.ENCODER,
                C2SVeinmineKeybindChange.DECODER,
                C2SVeinmineKeybindChange.HANDLER
        );
        NET.register(
                S2CSetShape.class,
                S2CSetShape.ENCODER,
                S2CSetShape.DECODER,
                S2CSetShape.HANDLER
        );

        Liteminer.LOGGER.info("Liteminer network initialized");
    }

    public static <T extends Packet<T>> void sendToServer(T packet) {
        NET.sendToServer(packet);
    }

    public static <T extends Packet<T>> void sendToPlayer(T packet, ServerPlayer player) {
        NET.sendToPlayer(packet, player);
    }
}
