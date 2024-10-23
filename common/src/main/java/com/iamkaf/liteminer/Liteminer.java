package com.iamkaf.liteminer;

import com.iamkaf.liteminer.registry.CreativeModeTabs;
import com.iamkaf.liteminer.registry.Items;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public final class Liteminer {
    public static final String MOD_ID = "liteminer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        LOGGER.info("Liteminer initializing.");

        // Registries
        Items.init();
        CreativeModeTabs.init();
    }

    /**
     * Creates resource location in the mod namespace with the given path.
     */
    public static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
