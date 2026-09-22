package com.iamkaf.liteminer.networking;

import com.iamkaf.amber.api.event.v1.events.common.WorldEvents;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

final class ServerHandshakeTest {
    @Test void clientWorldUnloadIsIgnored() {
        ServerHandshake.initialize();
        assertDoesNotThrow(() -> WorldEvents.WORLD_UNLOAD.invoker().onWorldUnload(null, null));
    }
}
