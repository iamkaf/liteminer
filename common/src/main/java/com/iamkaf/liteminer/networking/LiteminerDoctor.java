package com.iamkaf.liteminer.networking;

import com.iamkaf.amber.api.core.v2.AmberModInfo;
import com.iamkaf.amber.api.doctor.v1.Doctor;
import com.iamkaf.amber.api.doctor.v1.DoctorSection;
import com.iamkaf.amber.api.doctor.v1.DoctorStatus;
import com.iamkaf.amber.api.platform.v1.Platform;
import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.api.shape.LiteminerShapes;
import net.minecraft.network.chat.Component;

public final class LiteminerDoctor {
    private LiteminerDoctor() {}

    static AmberModInfo info() {
        var mod = Platform.getModInfo(Liteminer.MOD_ID);
        return new AmberModInfo(mod.id(), mod.name(), mod.version());
    }

    static Component text(String key, Object... args) {
        return Component.translatable("liteminer.doctor." + key, args);
    }

    static void initialization(DoctorSection section) {
        section.check("initialization", text("initialization"),
                LiteminerNetwork.isInitialized() ? DoctorStatus.OK : DoctorStatus.ERROR,
                text(LiteminerNetwork.isInitialized() ? "initialized" : "not_initialized"));
    }

    public static void initialize() {
        Doctor.registerServer(info(), (context, section) -> {
            initialization(section);
            if (context.player().isEmpty()) {
                section.check("connection", text("connection"), DoctorStatus.UNKNOWN, text("target_required"));
                return;
            }
            var player = context.player().get();
            var reply = ServerHandshake.snapshot(player);
            if (reply.isEmpty()) {
                section.check("connection", text("connection"), DoctorStatus.UNKNOWN, text("no_handshake"));
            } else {
                var state = reply.get();
                section.check("connection", text("connection"),
                        state.result() == MiningProtocol.Result.APPLIED || state.result() == MiningProtocol.Result.SERVER_CHANGED
                                ? DoctorStatus.OK : DoctorStatus.WARNING,
                        text("server_observed"));
                section.information("sequence", text("sequence"), Component.literal(Long.toString(state.sequence())));
            }
            ServerHandshake.requestSnapshot(player).ifPresent(request -> {
                section.information("peer", text("client_peer"), Component.literal(request.version()));
                section.information("protocol", text("protocol"), Component.literal(Integer.toString(request.protocol())));
                section.information("requested", text("requested"), text("state", request.active(), request.shape()));
            });
            var applied = Liteminer.instance.playerStateMap.get(player.getUUID());
            if (applied == null) {
                section.check("applied", text("applied"), DoctorStatus.UNKNOWN, text("no_state"));
                return;
            }
            section.information("applied", text("applied"), text("state", applied.getKeymappingState(),
                    LiteminerShapes.byIndex(applied.getShape()).orElseThrow().id().toString()));
        });
    }
}
