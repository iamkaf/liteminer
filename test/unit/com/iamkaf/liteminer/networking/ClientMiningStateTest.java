package com.iamkaf.liteminer.networking;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static com.iamkaf.amber.api.networking.v1.PeerAvailability.*;
import static com.iamkaf.liteminer.networking.ClientMiningState.Status.*;

final class ClientMiningStateTest {
    private static final String SHAPE = "liteminer:shapeless";

    private ClientMiningState connected() {
        var state = new ClientMiningState("client", SHAPE);
        state.discover(SUPPORTED, SUPPORTED, 0);
        return state;
    }

    private MiningProtocol.Reply applied(MiningProtocol.Request request, long revision) {
        return new MiningProtocol.Reply(request.nonce(), 1, "server", request.sequence(), revision,
                request.active(), request.shape(), MiningProtocol.Result.APPLIED);
    }

    @Test void pendingDiscoveryNeverClaimsMissingSupport() {
        var state = new ClientMiningState("client", SHAPE);
        state.discover(PENDING, ABSENT, 0);
        assertEquals(CHECKING, state.status());
        assertTrue(state.nextRequest(100_000).isEmpty());
        assertFalse(state.hasSupportWarning());
    }

    @Test void routineConfirmationDoesNotFlashAWarning() {
        var state = connected();
        state.desire(true, SHAPE, 0);
        var first = state.nextRequest(0).orElseThrow();
        assertFalse(state.hasSupportWarning());
        state.acknowledge(applied(first, 1));
        assertFalse(state.hasSupportWarning());

        state.desire(true, "addon:tunnel", 100);
        var update = state.nextRequest(100).orElseThrow();
        state.nextRequest(10_099);
        assertEquals(CHECKING, state.status());
        assertFalse(state.hasSupportWarning());
        state.nextRequest(10_100);
        assertEquals(DEGRADED, state.status());
        assertTrue(state.hasSupportWarning());
        state.acknowledge(applied(update, 2));
        assertFalse(state.hasSupportWarning());
    }

    @Test void absenceAndLegacyHaveDifferentMeanings() {
        var state = new ClientMiningState("client", SHAPE);
        state.discover(ABSENT, ABSENT, 0);
        assertEquals(UNAVAILABLE, state.status());
        assertTrue(state.hasSupportWarning());
        assertFalse(state.allowsInput());
        state.discover(ABSENT, SUPPORTED, 1);
        assertEquals(LEGACY, state.status());
        assertTrue(state.hasSupportWarning());
        assertTrue(state.allowsInput());
        assertFalse(state.hasConfirmation());
    }

    @Test void sendsLatestDesiredStateAfterDiscovery() {
        var state = new ClientMiningState("client", SHAPE);
        state.desire(true, "addon:tunnel", 1);
        state.discover(SUPPORTED, SUPPORTED, 2);
        var request = state.nextRequest(2).orElseThrow();
        assertTrue(request.active());
        assertEquals("addon:tunnel", request.shape());
        assertFalse(state.confirmedActive());
        assertEquals(CHECKING, state.status());
        assertTrue(state.acknowledge(applied(request, 1)));
        assertEquals(HEALTHY, state.status());
    }

    @Test void retriesAreBoundedAndLateRepliesRecover() {
        var state = connected();
        var initial = state.nextRequest(0).orElseThrow();
        assertTrue(state.nextRequest(1999).isEmpty());
        assertEquals(initial, state.nextRequest(2000).orElseThrow());
        assertTrue(state.nextRequest(4999).isEmpty());
        assertEquals(initial, state.nextRequest(5000).orElseThrow());
        assertTrue(state.nextRequest(9999).isEmpty());
        assertTrue(state.nextRequest(10000).isEmpty());
        assertEquals(UNCONFIRMED, state.status());
        assertTrue(state.hasSupportWarning());
        assertTrue(state.nextRequest(60000).isEmpty());
        state.acknowledge(applied(initial, 1));
        assertEquals(HEALTHY, state.status());
    }

    @Test void rapidChangesCannotExtendTheDeadline() {
        var state = connected();
        for (int i = 0; i < 11; i++) {
            state.desire(i % 2 == 0, SHAPE, i * 1000);
            state.nextRequest(i * 1000);
        }
        assertEquals(UNCONFIRMED, state.status());
    }

    @Test void lostUpdateAcknowledgmentRetainsLastConfirmedState() {
        var state = connected();
        state.desire(true, SHAPE, 0);
        state.acknowledge(applied(state.nextRequest(0).orElseThrow(), 1));
        state.desire(false, SHAPE, 10);
        var request = state.nextRequest(10).orElseThrow();
        state.nextRequest(10010);
        assertEquals(DEGRADED, state.status());
        assertTrue(state.confirmedActive());
        assertFalse(state.desiredActive());
        state.acknowledge(applied(request, 2));
        assertEquals(HEALTHY, state.status());
        assertFalse(state.confirmedActive());
    }

    @Test void obsoleteAndPreviousSessionRepliesCannotConfirmNewState() {
        var state = connected();
        var old = state.nextRequest(0).orElseThrow();
        state.desire(true, SHAPE, 1);
        assertFalse(state.acknowledge(applied(old, 1)));
        var current = state.nextRequest(1).orElseThrow();
        var foreign = new MiningProtocol.Request(UUID.randomUUID(), 1, "client", current.sequence(), true, SHAPE);
        assertFalse(state.acknowledge(applied(foreign, 2)));
        assertEquals(CHECKING, state.status());
    }

    @Test void rejectionIsNotHealthy() {
        var state = connected();
        var request = state.nextRequest(0).orElseThrow();
        state.acknowledge(new MiningProtocol.Reply(request.nonce(), 1, "server", request.sequence(), 1,
                false, SHAPE, MiningProtocol.Result.UNKNOWN_SHAPE));
        assertEquals(DEGRADED, state.status());
        assertEquals(MiningProtocol.Result.UNKNOWN_SHAPE, state.result());
    }

    @Test void incompatibleReplyIsExplainedWithoutConfirmation() {
        var state = connected();
        var request = state.nextRequest(0).orElseThrow();
        state.acknowledge(new MiningProtocol.Reply(request.nonce(), 2, "server", request.sequence(), 1,
                false, SHAPE, MiningProtocol.Result.INCOMPATIBLE));
        assertEquals(ClientMiningState.Status.INCOMPATIBLE, state.status());
        assertTrue(state.hasSupportWarning());
        assertFalse(state.hasConfirmation());
    }

    @Test void serverShapeChangeCannotBeRolledBackByDuplicateReply() {
        var state = connected();
        var request = state.nextRequest(0).orElseThrow();
        var first = applied(request, 1);
        state.acknowledge(first);
        state.acknowledge(new MiningProtocol.Reply(request.nonce(), 1, "server", request.sequence(), 2,
                false, "addon:tunnel", MiningProtocol.Result.SERVER_CHANGED));
        assertEquals("addon:tunnel", state.desiredShape());
        assertFalse(state.acknowledge(first));
        assertEquals("addon:tunnel", state.confirmedShape());
    }
    @Test void realInputAfterTimeoutCanRecoverWithoutHidingTheFailure() {
        var state = connected();
        state.nextRequest(0);
        state.nextRequest(10000);
        state.desire(true, SHAPE, 12000);
        var latest = state.nextRequest(12000).orElseThrow();
        assertEquals(UNCONFIRMED, state.status());
        assertTrue(latest.active());
        state.acknowledge(applied(latest, 1));
        assertEquals(HEALTHY, state.status());
    }
}
