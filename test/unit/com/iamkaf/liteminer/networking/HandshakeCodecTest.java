package com.iamkaf.liteminer.networking;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

final class HandshakeCodecTest {
    @Test void roundTripsRequestWithStableAddonShapeId() {
        var request = new HandshakeRequest(new MiningProtocol.Request(UUID.randomUUID(), 1, "client", 42,
                true, "addon:registered_in_a_different_order"));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            HandshakeRequest.ENCODER.encode(request, buffer);
            assertEquals(request, HandshakeRequest.DECODER.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }

    @Test void roundTripsEveryReplyOutcome() {
        for (var result : MiningProtocol.Result.values()) {
            var reply = new HandshakeReply(new MiningProtocol.Reply(UUID.randomUUID(), 1, "server", 7, 8,
                    false, "addon:shape", result));
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                HandshakeReply.ENCODER.encode(reply, buffer);
                assertEquals(reply, HandshakeReply.DECODER.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally { buffer.release(); }
        }
    }

    @Test void refusesOversizedVersionAndShapeBeforeSending() {
        for (boolean oversizedVersion : new boolean[]{true, false}) {
            var request = new HandshakeRequest(new MiningProtocol.Request(UUID.randomUUID(), 1,
                    oversizedVersion ? "v".repeat(MiningProtocol.MAX_VERSION_LENGTH + 1) : "client", 1, true,
                    oversizedVersion ? "addon:shape" : "s".repeat(MiningProtocol.MAX_SHAPE_LENGTH + 1)));
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try { assertThrows(RuntimeException.class, () -> HandshakeRequest.ENCODER.encode(request, buffer)); }
            finally { buffer.release(); }
        }
    }

    @Test void refusesTruncatedPackets() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeUUID(UUID.randomUUID());
            assertThrows(IndexOutOfBoundsException.class, () -> HandshakeReply.DECODER.decode(buffer));
        } finally { buffer.release(); }
    }
}
