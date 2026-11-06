package com.iamkaf.liteminer.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
//? if <26.3
//import org.lwjgl.glfw.GLFW;

/**
 * Key mapping construction. Key categories became registered objects in 1.21.9, and key types and codes changed
 * in 26.3.
 */
public final class KeyMappings {
    private KeyMappings() {
    }

    /** A keyboard binding for the grave accent key under the given category. */
    public static KeyMapping graveAccent(String name, Identifier category) {
        //? if >=26.3 {
        return new KeyMapping(name, InputConstants.Type.KEYBOARD, InputConstants.KEY_GRAVE,
                KeyMapping.Category.register(category));
        //?} else if >=1.21.9 {
        /*return new KeyMapping(name, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT,
                KeyMapping.Category.register(category));
        *///?} else {
        /*return new KeyMapping(name, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT,
                "key.category." + category.getNamespace() + "." + category.getPath());
        *///?}
    }
}
