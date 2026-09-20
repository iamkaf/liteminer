package com.iamkaf.liteminer.networking;

import com.iamkaf.amber.api.networking.v1.*;

public record HandshakeReply(MiningProtocol.Reply value) implements Packet<HandshakeReply> {
    public static final PacketEncoder<HandshakeReply> ENCODER = (packet, buffer) -> {
        var value = packet.value();
        buffer.writeUUID(value.nonce());
        buffer.writeVarInt(value.protocol());
        buffer.writeUtf(value.version(), MiningProtocol.MAX_VERSION_LENGTH);
        buffer.writeLong(value.sequence());
        buffer.writeLong(value.revision());
        buffer.writeBoolean(value.active());
        buffer.writeUtf(value.shape(), MiningProtocol.MAX_SHAPE_LENGTH);
        buffer.writeEnum(value.result());
    };
    public static final PacketDecoder<HandshakeReply> DECODER = buffer -> new HandshakeReply(
            new MiningProtocol.Reply(buffer.readUUID(), buffer.readVarInt(),
                    buffer.readUtf(MiningProtocol.MAX_VERSION_LENGTH), buffer.readLong(), buffer.readLong(),
                    buffer.readBoolean(), buffer.readUtf(MiningProtocol.MAX_SHAPE_LENGTH),
                    buffer.readEnum(MiningProtocol.Result.class)));
    public static final PacketHandler<HandshakeReply> HANDLER = (packet, context) -> {
        if (context.isClientSide()) {
            var player = context.getPlayer();
            context.execute(() -> ClientHandshake.receive(player, packet.value()));
        }
    };
}
