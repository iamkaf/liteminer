package com.iamkaf.liteminer.neoforge;

import net.neoforged.fml.common.Mod;

import com.iamkaf.liteminer.Liteminer;

@Mod(Liteminer.MOD_ID)
public final class LiteminerNeoForge {
    public LiteminerNeoForge() {
        // Run our common setup.
        Liteminer.init();
    }
}
