package com.iamkaf.liteminer.networking;

import java.util.UUID;

/** Immutable v1 messages. Display versions never determine wire compatibility. */
public final class MiningProtocol {
    public static final int REVISION = 1;
    public static final int MAX_VERSION_LENGTH = 128;
    public static final int MAX_SHAPE_LENGTH = 256;

    private MiningProtocol() {}

    public enum Result {
        APPLIED, UNKNOWN_SHAPE, INCOMPATIBLE, STALE, WRONG_SESSION, SERVER_CHANGED
    }

    public record Request(UUID nonce, int protocol, String version, long sequence, boolean active, String shape) {}
    public record Reply(UUID nonce, int protocol, String version, long sequence, long revision,
                        boolean active, String shape, Result result) {}
}
