package com.example.template;

import com.iamkaf.amber.api.core.v2.AmberInitializer;
import com.example.template.platform.Services;

/**
 * Common entry point for the Template mod.
 */
public final class TemplateMod {

    private TemplateMod() {
    }

    /**
     * Called during mod initialization for all loaders.
     */
    public static void init() {
        Constants.LOG.info("Initializing {} on {}...", Constants.MOD_NAME, Services.PLATFORM.getPlatformName());
        AmberInitializer.initialize(Constants.MOD_ID);
    }
}
