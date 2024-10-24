package com.iamkaf.liteminer;

import java.util.UUID;

public class LiteminerPlayerState {
    private final UUID uuid;
    private boolean keymappingState = false;

    public LiteminerPlayerState(UUID playerUuid) {
        this.uuid = playerUuid;
    }

    public boolean getKeymappingState() {
        return keymappingState;
    }

    public void setKeymappingState(boolean keymappingState) {
        this.keymappingState = keymappingState;
    }

    public UUID getUuid() {
        return uuid;
    }
}
