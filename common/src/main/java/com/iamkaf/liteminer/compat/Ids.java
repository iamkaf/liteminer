package com.iamkaf.liteminer.compat;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/** Identifier factories. The constructor went private in Minecraft 1.21. */
public final class Ids {
    private Ids() {
    }

    public static Identifier of(String namespace, String path) {
        //? if >=1.21 {
        return Identifier.fromNamespaceAndPath(namespace, path);
        //?} else {
        /*return new Identifier(namespace, path);
        *///?}
    }

    public static Identifier parse(String id) {
        //? if >=1.21 {
        return Identifier.parse(id);
        //?} else {
        /*return new Identifier(id);
        *///?}
    }

    public static @Nullable Identifier tryParse(String id) {
        return Identifier.tryParse(id);
    }
}
