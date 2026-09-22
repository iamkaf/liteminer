package com.iamkaf.liteminer.networking;

import com.iamkaf.amber.api.event.v1.events.common.PlayerEvents;
import com.iamkaf.amber.api.event.v1.events.common.WorldEvents;
import com.iamkaf.amber.api.networking.v1.PeerAvailability;
import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.api.shape.LiteminerShapes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Server-thread state, tied to the actual player connection. */
public final class ServerHandshake {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private ServerHandshake() {}

    public static void initialize() {
        PlayerEvents.PLAYER_LEAVE.register(player -> {
            SESSIONS.remove(player.getUUID());
            Liteminer.instance.playerStateMap.remove(player.getUUID());
        });
        WorldEvents.WORLD_UNLOAD.register((server, level) -> {
            // NeoForge also forwards client-world unloads, which have no server.
            if (server != null && level == server.overworld()) {
                SESSIONS.clear();
                Liteminer.instance.playerStateMap.clear();
            }
        });
        PlayerEvents.PLAYER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            Session session = SESSIONS.get(newPlayer.getUUID());
            if (session != null) {
                session.connection = newPlayer.connection;
                synchronize(newPlayer);
            }
        });
    }

    public static boolean negotiated(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        return session != null && session.connection == player.connection;
    }

    public static Optional<MiningProtocol.Request> requestSnapshot(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        return session != null && session.connection == player.connection ? Optional.of(session.request) : Optional.empty();
    }

    public static Optional<MiningProtocol.Reply> snapshot(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        return session != null && session.connection == player.connection ? Optional.of(session.reply) : Optional.empty();
    }

    public static void receive(ServerPlayer player, MiningProtocol.Request request) {
        if (LiteminerNetwork.HANDSHAKE.playerAvailability(player) != PeerAvailability.SUPPORTED) return;
        Session session = SESSIONS.get(player.getUUID());
        if (session != null && session.connection != player.connection) {
            SESSIONS.remove(player.getUUID());
            session = null;
        }
        if (session != null && !session.request.nonce().equals(request.nonce())) {
            send(player, reply(player, request, session.reply.revision(), MiningProtocol.Result.WRONG_SESSION));
            return;
        }
        if (session != null && request.sequence() <= session.request.sequence()) {
            send(player, request.equals(session.request) ? session.reply
                    : reply(player, request, session.reply.revision(), MiningProtocol.Result.STALE));
            return;
        }
        MiningProtocol.Result result;
        Identifier shape = Identifier.tryParse(request.shape());
        int index = shape == null ? -1 : LiteminerShapes.indexOf(shape);
        if (request.protocol() != MiningProtocol.REVISION) {
            result = MiningProtocol.Result.INCOMPATIBLE;
        } else if (request.sequence() < 1) {
            result = MiningProtocol.Result.STALE;
        } else if (index < 0) {
            result = MiningProtocol.Result.UNKNOWN_SHAPE;
        } else {
            Liteminer.instance.onKeymappingStateChange(player, request.active(), index);
            result = MiningProtocol.Result.APPLIED;
        }
        long revision = session == null ? 1 : session.reply.revision() + 1;
        MiningProtocol.Reply response = reply(player, request, revision, result);
        SESSIONS.put(player.getUUID(), new Session(player.connection, request, response));
        send(player, response);
    }

    /** Synchronize server-driven changes through the negotiated state path. */
    public static boolean synchronize(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.connection != player.connection) return false;
        session.reply = reply(player, session.request, session.reply.revision() + 1, MiningProtocol.Result.SERVER_CHANGED);
        send(player, session.reply);
        return true;
    }

    private static MiningProtocol.Reply reply(ServerPlayer player, MiningProtocol.Request request,
                                              long revision, MiningProtocol.Result result) {
        var state = Liteminer.instance.getPlayerState(player);
        String shape = LiteminerShapes.byIndex(state.getShape()).orElseThrow().id().toString();
        return new MiningProtocol.Reply(request.nonce(), MiningProtocol.REVISION, LiteminerNetwork.version(),
                request.sequence(), revision, state.getKeymappingState(), shape, result);
    }

    private static void send(ServerPlayer player, MiningProtocol.Reply reply) {
        LiteminerNetwork.HANDSHAKE.sendToPlayer(new HandshakeReply(reply), player);
    }

    private static final class Session {
        private ServerGamePacketListenerImpl connection;
        private final MiningProtocol.Request request;
        private MiningProtocol.Reply reply;
        private Session(ServerGamePacketListenerImpl connection, MiningProtocol.Request request, MiningProtocol.Reply reply) {
            this.connection = connection;
            this.request = request;
            this.reply = reply;
        }
    }
}
