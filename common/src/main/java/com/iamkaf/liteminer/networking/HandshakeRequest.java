package com.iamkaf.liteminer.networking;

import com.iamkaf.amber.api.networking.v1.*;
import net.minecraft.server.level.ServerPlayer;

public record HandshakeRequest(MiningProtocol.Request value) implements Packet<HandshakeRequest> {
    public static final PacketEncoder<HandshakeRequest> ENCODER = (packet, buffer) -> {
        var value = packet.value();
        buffer.writeUUID(value.nonce());
        buffer.writeVarInt(value.protocol());
        buffer.writeUtf(value.version(), MiningProtocol.MAX_VERSION_LENGTH);
        buffer.writeLong(value.sequence());
        buffer.writeBoolean(value.active());
        buffer.writeUtf(value.shape(), MiningProtocol.MAX_SHAPE_LENGTH);
    };
    public static final PacketDecoder<HandshakeRequest> DECODER = buffer -> new HandshakeRequest(
            new MiningProtocol.Request(buffer.readUUID(), buffer.readVarInt(),
                    buffer.readUtf(MiningProtocol.MAX_VERSION_LENGTH), buffer.readLong(),
                    buffer.readBoolean(), buffer.readUtf(MiningProtocol.MAX_SHAPE_LENGTH)));
    public static final PacketHandler<HandshakeRequest> HANDLER = (packet, context) -> {
        if (context.isServerSide() && context.getPlayer() instanceof ServerPlayer player) {
            context.execute(() -> ServerHandshake.receive(player, packet.value()));
        }
    };
}
