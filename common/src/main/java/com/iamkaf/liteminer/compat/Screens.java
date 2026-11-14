package com.iamkaf.liteminer.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.UnaryOperator;

/** Screen opening. The current screen moved to {@code Minecraft.gui} in 26.2. */
public final class Screens {
    private Screens() {
    }

    /** Opens the screen {@code factory} builds, with the current screen as its parent. */
    public static void openOverCurrent(UnaryOperator<Screen> factory) {
        Minecraft minecraft = Minecraft.getInstance();
        //? if >=26.2 {
        minecraft.gui.setScreen(factory.apply(minecraft.gui.screen()));
        //?} else {
        /*minecraft.setScreen(factory.apply(minecraft.screen));
        *///?}
    }
}
