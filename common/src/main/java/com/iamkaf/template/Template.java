package com.iamkaf.template;

import com.iamkaf.registry.CreativeModeTabs;
import com.iamkaf.registry.Items;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public final class Template {
    public static final String MOD_ID = "template";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        LOGGER.info("Kaf template.");

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
