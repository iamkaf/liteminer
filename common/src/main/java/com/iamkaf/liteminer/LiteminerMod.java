package com.iamkaf.liteminer;

import com.iamkaf.liteminer.platform.Services;

/**
 * Common entry point for the Liteminer mod.
 * Replace the contents with your own implementation.
 */
public class LiteminerMod {

    /**
     * Called during mod initialization for all loaders.
     */
    public static void init() {
        Constants.LOG.info("Initializing {} on {}...", Constants.MOD_NAME, Services.PLATFORM.getPlatformName());
    }
}
