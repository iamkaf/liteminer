package com.iamkaf.liteminer;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class LiteminerClient {
    public static KeyMapping KEY_MAPPING = new KeyMapping(
            "key.liteminer.veinmine",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            "key.categories.liteminer"
    );

    public static void init() {
        KeyMappingRegistry.register(KEY_MAPPING);
    }
}
