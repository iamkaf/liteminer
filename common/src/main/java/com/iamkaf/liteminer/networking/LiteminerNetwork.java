package com.iamkaf.liteminer.networking;

import com.iamkaf.amber.api.networking.v1.NetworkChannel;
import com.iamkaf.amber.api.networking.v1.Packet;
import com.iamkaf.liteminer.Liteminer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class LiteminerNetwork {
    public static final NetworkChannel NET =
            NetworkChannel.createOptional(Identifier.fromNamespaceAndPath(Liteminer.MOD_ID, "main"));

    private static boolean initialized;

    public static final NetworkChannel HANDSHAKE = NetworkChannel.createOptional(
            Identifier.fromNamespaceAndPath(Liteminer.MOD_ID, "handshake_v1"));

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

        HANDSHAKE.register(HandshakeRequest.class, HandshakeRequest.ENCODER, HandshakeRequest.DECODER, HandshakeRequest.HANDLER);
        HANDSHAKE.register(HandshakeReply.class, HandshakeReply.ENCODER, HandshakeReply.DECODER, HandshakeReply.HANDLER);
        ServerHandshake.initialize();
        LiteminerDoctor.initialize();
        initialized = true;
        Liteminer.LOGGER.info("Liteminer network initialized");
    }

    public static boolean isInitialized() { return initialized; }

    public static String version() {
        return com.iamkaf.amber.api.platform.v1.Platform.getModVersion(Liteminer.MOD_ID);
    }

    public static <T extends Packet<T>> void sendToServer(T packet) {
        if (packet instanceof C2SVeinmineKeybindChange change) {
            ClientHandshake.request(change.keybindState(), change.shape());
        } else {
            NET.sendToServer(packet);
        }
    }

    public static <T extends Packet<T>> void sendToPlayer(T packet, ServerPlayer player) {
        if (packet instanceof S2CSetShape && ServerHandshake.synchronize(player)) return;
        NET.sendToPlayer(packet, player);
    }
}
