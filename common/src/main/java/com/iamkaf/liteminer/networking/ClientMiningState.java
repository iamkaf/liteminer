package com.iamkaf.liteminer.networking;

import com.iamkaf.amber.api.networking.v1.PeerAvailability;
import java.util.Optional;
import java.util.UUID;

/** Connection-owned desired and confirmed state. Time arguments use a monotonic local clock. */
public final class ClientMiningState {
    public enum Status {
        DISCONNECTED, CHECKING, HEALTHY, LEGACY, UNAVAILABLE, INCOMPATIBLE, UNCONFIRMED, DEGRADED
    }

    private final UUID nonce = UUID.randomUUID();
    private final String version;
    private boolean desiredActive;
    private String desiredShape;
    private boolean confirmedActive;
    private String confirmedShape;
    private String peerVersion = "";
    private int peerProtocol;
    private long sequence;
    private long revision = -1;
    private long pendingSince;
    private long requestStarted;
    private int sends;
    private boolean pending;
    private boolean capable;
    private boolean everConfirmed;
    private Status status = Status.CHECKING;
    private MiningProtocol.Result result = MiningProtocol.Result.APPLIED;

    public ClientMiningState(String version, String shape) {
        this.version = version;
        desiredShape = shape;
        confirmedShape = "";
    }

    public void discover(PeerAvailability handshake, PeerAvailability legacy, long now) {
        if (handshake == PeerAvailability.SUPPORTED) {
            if (!capable) {
                capable = true;
                request(now);
            }
            return;
        }
        if (capable) {
            if (handshake == PeerAvailability.ABSENT || handshake == PeerAvailability.INCOMPATIBLE) {
                status = handshake == PeerAvailability.INCOMPATIBLE ? Status.INCOMPATIBLE : Status.DEGRADED;
            }
            return;
        }
        if (handshake == PeerAvailability.INCOMPATIBLE || legacy == PeerAvailability.INCOMPATIBLE) {
            status = Status.INCOMPATIBLE;
        } else if (handshake == PeerAvailability.PENDING || legacy == PeerAvailability.PENDING) {
            status = Status.CHECKING;
        } else {
            status = legacy == PeerAvailability.SUPPORTED ? Status.LEGACY : Status.UNAVAILABLE;
        }
    }

    public boolean desire(boolean active, String shape, long now) {
        if (active == desiredActive && shape.equals(desiredShape)) return false;
        desiredActive = active;
        desiredShape = shape;
        if (capable) request(now);
        return true;
    }

    private void request(long now) {
        sequence++;
        if (!pending) pendingSince = now;
        pending = true;
        requestStarted = now;
        sends = 0;
        status = now - pendingSince >= 10_000
                ? (everConfirmed ? Status.DEGRADED : Status.UNCONFIRMED) : Status.CHECKING;
        result = MiningProtocol.Result.APPLIED;
    }

    public Optional<MiningProtocol.Request> nextRequest(long now) {
        if (!capable || !pending || status == Status.INCOMPATIBLE) return Optional.empty();
        long age = now - pendingSince;
        if (age >= 10_000) {
            status = everConfirmed ? Status.DEGRADED : Status.UNCONFIRMED;
            if (sends != 0) return Optional.empty();
        }
        long requestAge = now - requestStarted;
        if (sends == 0 || sends == 1 && requestAge >= 2_000 || sends == 2 && requestAge >= 5_000) {
            sends++;
            return Optional.of(new MiningProtocol.Request(nonce, MiningProtocol.REVISION, version,
                    sequence, desiredActive, desiredShape));
        }
        return Optional.empty();
    }

    public boolean acknowledge(MiningProtocol.Reply reply) {
        if (!capable || !nonce.equals(reply.nonce()) || reply.sequence() != sequence || reply.revision() < revision) {
            return false;
        }
        peerVersion = reply.version();
        peerProtocol = reply.protocol();
        result = reply.result();
        if (reply.protocol() != MiningProtocol.REVISION || result == MiningProtocol.Result.INCOMPATIBLE) {
            status = Status.INCOMPATIBLE;
            pending = false;
            return true;
        }
        // A duplicate cannot undo a more recent server-initiated change.
        if (reply.revision() == revision && !pending) return false;
        revision = reply.revision();
        confirmedActive = reply.active();
        confirmedShape = reply.shape();
        pending = false;
        if (result == MiningProtocol.Result.SERVER_CHANGED) {
            desiredActive = confirmedActive;
            desiredShape = confirmedShape;
        }
        if ((result == MiningProtocol.Result.APPLIED || result == MiningProtocol.Result.SERVER_CHANGED)
                && desiredActive == confirmedActive && desiredShape.equals(confirmedShape)) {
            status = Status.HEALTHY;
            everConfirmed = true;
        } else {
            status = Status.DEGRADED;
        }
        return true;
    }

    public Status status() { return status; }
    public boolean desiredActive() { return desiredActive; }
    public String desiredShape() { return desiredShape; }
    public boolean confirmedActive() { return confirmedActive; }
    public String confirmedShape() { return confirmedShape; }
    public String peerVersion() { return peerVersion; }
    public int peerProtocol() { return peerProtocol; }
    public MiningProtocol.Result result() { return result; }
    public long sequence() { return sequence; }
    public boolean capable() { return capable; }
    public boolean hasConfirmation() { return everConfirmed; }
    public boolean allowsInput() { return status != Status.UNAVAILABLE && status != Status.INCOMPATIBLE; }
}
