package com.iamkaf.liteminer.compat;

import com.iamkaf.amber.api.event.v1.events.common.client.HudEvents;
import com.iamkaf.amber.api.functions.v1.ClientFunctions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** HUD drawing. The graphics type changed in 1.20, 1.21.6 (pose stack), and 26.1, and hiding the HUD in 26.2. */
public final class HudCompat {
    private HudCompat() {
    }

    public interface Canvas {
        void text(Font font, Component text, int x, int y, int color);

        /** Runs {@code draw} with everything it draws scaled by {@code scale}. */
        void scaled(float scale, Runnable draw);
    }

    public static void register(Consumer<Canvas> renderer) {
        HudEvents.RENDER_HUD.register((graphics, tickCounter) -> renderer.accept(new Canvas() {
            @Override
            public void text(Font font, Component text, int x, int y, int color) {
                ClientFunctions.renderText(graphics, font, text, x, y, color);
            }

            @Override
            public void scaled(float scale, Runnable draw) {
                //? if >=1.21.6 {
                var pose = graphics.pose();
                pose.pushMatrix();
                pose.scale(scale, scale);
                draw.run();
                pose.popMatrix();
                //?} else if >=1.20 {
                /*var pose = graphics.pose();
                pose.pushPose();
                pose.scale(scale, scale, 1.0F);
                draw.run();
                pose.popPose();
                *///?} else {
                /*graphics.pushPose();
                graphics.scale(scale, scale, 1.0F);
                draw.run();
                graphics.popPose();
                *///?}
            }
        }));
    }

    public static boolean isHidden() {
        //? if >=26.2 {
        return Minecraft.getInstance().gameRenderer.gameRenderState().guiRenderState.isHudHidden;
        //?} else {
        /*return Minecraft.getInstance().options.hideGui;
        *///?}
    }
}
