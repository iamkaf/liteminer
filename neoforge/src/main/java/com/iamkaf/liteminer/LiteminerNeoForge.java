package com.iamkaf.liteminer;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class LiteminerNeoForge {
    public LiteminerNeoForge(IEventBus eventBus) {
        LiteminerMod.init();
    }
}