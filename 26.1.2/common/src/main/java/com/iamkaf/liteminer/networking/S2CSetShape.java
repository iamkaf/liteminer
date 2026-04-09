package com.iamkaf.liteminer.networking;

import com.iamkaf.amber.api.networking.v1.Packet;
import com.iamkaf.amber.api.networking.v1.PacketDecoder;
import com.iamkaf.amber.api.networking.v1.PacketEncoder;
import com.iamkaf.amber.api.networking.v1.PacketHandler;
import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.LiteminerClient;

public record S2CSetShape(int shape) implements Packet<S2CSetShape> {
    public static final PacketEncoder<S2CSetShape> ENCODER = (packet, buffer) -> buffer.writeInt(packet.shape);

    public static final PacketDecoder<S2CSetShape> DECODER = buffer -> new S2CSetShape(buffer.readInt());

    public static final PacketHandler<S2CSetShape> HANDLER = (packet, context) -> {
        if (context.isServerSide()) {
            Liteminer.LOGGER.warn("Received S2CSetShape on server side - this should not happen");
            return;
        }

        context.execute(() -> {
            LiteminerClient.shapes.setCurrentIndex(packet.shape);
            Liteminer.LOGGER.debug("Set client Liteminer shape to {}", packet.shape);
        });
    };
}
