package com.iamkaf.liteminer;

import java.util.UUID;

public class LiteminerPlayerState {
    private final UUID uuid;
    private boolean keymappingState = false;
    private int shape = 0;

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

    public int getShape() {
        return shape;
    }

    public void setShape(int shape) {
        this.shape = shape;
    }
}
