package com.iamkaf.liteminer.compat;

import net.minecraft.network.chat.MutableComponent;
//? if >=1.19 {
import net.minecraft.network.chat.Component;
//?} else {
/*import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
*///?}

/** Component factories. Minecraft 1.18.2 predates {@code Component.translatable} and friends. */
public final class Texts {
    private Texts() {
    }

    public static MutableComponent translatable(String key, Object... args) {
        //? if >=1.19 {
        return Component.translatable(key, args);
        //?} else {
        /*return new TranslatableComponent(key, args);
        *///?}
    }

    public static MutableComponent literal(String text) {
        //? if >=1.19 {
        return Component.literal(text);
        //?} else {
        /*return new TextComponent(text);
        *///?}
    }

    public static MutableComponent empty() {
        //? if >=1.19 {
        return Component.empty();
        //?} else {
        /*return new TextComponent("");
        *///?}
    }
}
