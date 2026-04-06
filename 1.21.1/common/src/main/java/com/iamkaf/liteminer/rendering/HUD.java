package com.iamkaf.liteminer.rendering;

import com.iamkaf.amber.api.player.FeedbackHelper;
import com.iamkaf.liteminer.LiteminerClient;
import com.iamkaf.liteminer.networking.LiteminerNetwork;
import dev.architectury.event.EventResult;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class HUD {
    public static void onRenderHUD(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!LiteminerClient.CONFIG.showHUD.get()) {
            return;
        }

        if (Minecraft.getInstance().options.hideGui) {
            return;
        }

        int selectedBlockCount = LiteminerClient.selectedBlocks.size();

        if (selectedBlockCount == 0) {
            return;
        }

        if (!LiteminerClient.isVeinMining() || !LiteminerClient.isTargetingABlock()) {
            return;
        }

        Font font = LiteminerClient.mc.font;

        int lineHeight = 10;
        float scale = LiteminerClient.CONFIG.hud_scale.get().floatValue();

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        int centerWidth = (int) (width / 2f / scale);
        int centerHeight = (int) (height / 2f / scale);

        int xOffset = (int) (5 / scale);
        int yOffset = (int) (-10 / scale);

        String selectedBlocksLabel = Component.translatable(
                selectedBlockCount > 1 ? "hud.liteminer.selected_blocks" : "hud.liteminer" +
                        ".selected_blocks_singular",
                selectedBlockCount
        ).getString();

        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.scale(scale, scale, 1f);

        guiGraphics.drawString(font,
                selectedBlocksLabel,
                centerWidth + xOffset,
                centerHeight + yOffset,
                0xFFFFFF
        );
        guiGraphics.drawString(font,
                LiteminerClient.shapes.getCurrentItem().toString(),
                centerWidth + xOffset,
                centerHeight + yOffset + lineHeight,
                0xFFFFFF
        );

        pose.popPose();
    }

    public static EventResult onMouseScroll(Minecraft minecraft, double x, double y) {
        if (LiteminerClient.isVeinMining()) {
            if (y != 0) {
                if (y > 0) {
                    LiteminerClient.shapes.previousItem();
                } else if (y < 0) {
                    LiteminerClient.shapes.nextItem();
                }
                new LiteminerNetwork.Messages.C2SVeinmineKeybindChange(LiteminerClient.isVeinMining(),
                        LiteminerClient.shapes.getCurrentIndex()
                ).sendToServer();
            }
            if (!LiteminerClient.CONFIG.showHUD.get()) {
                assert minecraft.player != null;
                FeedbackHelper.actionBarMessage(minecraft.player, Component.translatable(
                        "hud.liteminer.changed_shape",
                        LiteminerClient.shapes.getCurrentItem().toString()
                ));
            }
            return EventResult.interruptFalse();
        }

        return EventResult.pass();
    }
}
