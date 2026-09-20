package com.iamkaf.liteminer.networking;

import com.iamkaf.amber.api.doctor.v1.ClientDoctor;
import com.iamkaf.amber.api.doctor.v1.DoctorStatus;
import net.minecraft.network.chat.Component;
import static com.iamkaf.liteminer.networking.LiteminerDoctor.text;

public final class ClientLiteminerDoctor {
    private ClientLiteminerDoctor() {}

    public static void initialize() {
        ClientDoctor.register(LiteminerDoctor.info(), (context, section) -> {
            LiteminerDoctor.initialization(section);
            var snapshot = ClientHandshake.snapshot();
            if (snapshot.isEmpty()) {
                section.check("connection", text("connection"), DoctorStatus.UNKNOWN,
                        Component.translatable("liteminer.connection.disconnected"));
                return;
            }
            var state = snapshot.get();
            DoctorStatus status = switch (state.status()) {
                case HEALTHY -> DoctorStatus.OK;
                case UNAVAILABLE, INCOMPATIBLE -> DoctorStatus.ERROR;
                case DEGRADED, UNCONFIRMED -> DoctorStatus.WARNING;
                default -> DoctorStatus.UNKNOWN;
            };
            section.check("connection", text("connection"), status, ClientHandshake.explanation(state));
            section.information("requested", text("requested"), text("state", state.desiredActive(), state.desiredShape()));
            if (state.hasConfirmation()) {
                section.information("confirmed", text("confirmed"), text("state", state.confirmedActive(), state.confirmedShape()));
            }
            if (!state.peerVersion().isEmpty()) {
                section.information("peer", text("peer"), Component.literal(state.peerVersion()));
                section.information("protocol", text("protocol"), Component.literal(Integer.toString(state.peerProtocol())));
            }
        });
    }
}
