package com.example.template;

import net.fabricmc.api.ModInitializer;

/**
 * Fabric entry point.
 */
public class TemplateFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        TemplateMod.init();
    }
}
