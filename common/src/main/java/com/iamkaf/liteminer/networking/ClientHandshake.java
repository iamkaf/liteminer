package com.iamkaf.liteminer.networking;

import com.iamkaf.amber.api.functions.v1.PlayerFunctions;
import com.iamkaf.liteminer.LiteminerClient;
import com.iamkaf.liteminer.api.shape.LiteminerShapes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import java.util.Locale;
import java.util.Optional;

/** Client-only connection lifecycle, transport, and feedback. */
public final class ClientHandshake {
    private static ClientPacketListener connection;
    private static ClientMiningState state;
    private static boolean warned;
    private static boolean legacySent;
    private ClientHandshake() {}

    private static long now() { return System.nanoTime() / 1_000_000L; }

    public static Optional<ClientMiningState> snapshot() { return Optional.ofNullable(state); }

    public static void tick() {
        var client = Minecraft.getInstance();
        var current = client.getConnection();
        if (current != connection || current == null && state != null) {
            connection = current;
            state = current == null ? null : new ClientMiningState(LiteminerNetwork.version(),
                    LiteminerClient.shapes.getCurrentItem().id().toString());
            warned = false;
            legacySent = false;
            LiteminerClient.resetInput();
        }
        if (state == null || client.player == null) return;
        state.discover(LiteminerNetwork.HANDSHAKE.serverAvailability(), LiteminerNetwork.NET.serverAvailability(), now());
        flush();
    }

    public static void request(boolean active, int shapeIndex) {
        tick();
        if (state == null) return;
        String shape = LiteminerShapes.byIndex(shapeIndex).orElseThrow().id().toString();
        if (state.desire(active, shape, now())) legacySent = false;
        flush();
    }

    private static void flush() {
        state.nextRequest(now()).ifPresent(request -> LiteminerNetwork.HANDSHAKE.sendToServer(new HandshakeRequest(request)));
        if (state.status() == ClientMiningState.Status.LEGACY && !legacySent) {
            int shape = LiteminerShapes.indexOf(Identifier.parse(state.desiredShape()));
            LiteminerNetwork.NET.sendToServer(new C2SVeinmineKeybindChange(state.desiredActive(), shape));
            legacySent = true;
        }
        feedback();
    }

    public static void receive(Player recipient, MiningProtocol.Reply reply) {
        var client = Minecraft.getInstance();
        if (state == null || client.getConnection() != connection || client.player != recipient) return;
        Identifier shape = Identifier.tryParse(reply.shape());
        if (shape == null || LiteminerShapes.indexOf(shape) < 0) {
            reply = new MiningProtocol.Reply(reply.nonce(), reply.protocol(), reply.version(), reply.sequence(),
                    reply.revision(), reply.active(), reply.shape(), MiningProtocol.Result.UNKNOWN_SHAPE);
        }
        if (state.acknowledge(reply) && state.status() == ClientMiningState.Status.HEALTHY) {
            LiteminerClient.shapes.setCurrentIndex(LiteminerShapes.indexOf(Identifier.parse(state.confirmedShape())));
            warned = false;
        }
        feedback();
    }

    public static boolean allowsMining() {
        return state != null && (state.status() == ClientMiningState.Status.HEALTHY
                ? state.confirmedActive() : state.status() == ClientMiningState.Status.LEGACY && state.desiredActive());
    }

    /** A visible preview is not proof that the server supports mining. */
    public static Optional<Component> hudMessage() {
        if (!hasSupportWarning()) return Optional.empty();
        return Optional.of(Component.translatable("liteminer.connection.short."
                + state.status().name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.RED));
    }

    /** Keep the configured opacity for both visible and through-wall outlines. */
    public static int highlightColor(int configuredColor) {
        return hasSupportWarning() ? (configuredColor & 0xFF000000) | 0xDC143C : configuredColor;
    }

    private static boolean hasSupportWarning() {
        return state != null && state.hasSupportWarning();
    }

    public static Component explanation(ClientMiningState state) {
        if (state.status() == ClientMiningState.Status.DEGRADED && state.result() != MiningProtocol.Result.APPLIED) {
            return Component.translatable("liteminer.connection.result." + state.result().name().toLowerCase(Locale.ROOT));
        }
        return Component.translatable("liteminer.connection." + state.status().name().toLowerCase(Locale.ROOT));
    }

    private static void feedback() {
        var player = Minecraft.getInstance().player;
        if (player == null || state == null) return;
        if (state.status() == ClientMiningState.Status.HEALTHY) {
            warned = false;
            return;
        }
        if (!state.desiredActive()) return;
        if (state.status() != ClientMiningState.Status.CHECKING && !warned) {
            PlayerFunctions.sendMessage(player, explanation(state).copy().append(" ")
                    .append(Component.translatable("liteminer.connection.doctor_hint")));
            warned = true;
        }
    }
}
