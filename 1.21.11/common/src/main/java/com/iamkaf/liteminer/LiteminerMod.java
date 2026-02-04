package com.iamkaf.liteminer;

import com.iamkaf.amber.api.core.v2.AmberInitializer;
import com.iamkaf.liteminer.platform.Services;

/**
 * Common entry point for Liteminer.
 */
public final class LiteminerMod {

    private LiteminerMod() {
    }

    /**
     * Called during mod initialization for all loaders.
     */
    public static void init() {
        Constants.LOG.info("Initializing {} on {}...", Constants.MOD_NAME, Services.PLATFORM.getPlatformName());
        AmberInitializer.initialize(Constants.MOD_ID);
        new Liteminer();
        Liteminer.init();
    }
}
