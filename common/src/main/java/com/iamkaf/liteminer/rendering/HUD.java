package com.iamkaf.liteminer.rendering;

import com.iamkaf.amber.api.player.FeedbackHelper;
import com.iamkaf.liteminer.LiteminerClient;
import com.iamkaf.liteminer.networking.C2SVeinmineKeybindChange;
import com.iamkaf.liteminer.networking.LiteminerNetwork;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import org.joml.Matrix3x2fStack;

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

        Font font = Minecraft.getInstance().font;

        int lineHeight = 10;
        float scale = LiteminerClient.CONFIG.hud_scale.get().floatValue();

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        int centerWidth = (int) (width / 2f / scale);
        int centerHeight = (int) (height / 2f / scale);

        int xOffset = (int) (5 / scale);
        int yOffset = (int) (-10 / scale);

        Component selectedBlocksLabel = Component.translatable(
                selectedBlockCount > 1 ? "hud.liteminer.selected_blocks" : "hud.liteminer" + ".selected_blocks_singular",
                selectedBlockCount
        );

        Matrix3x2fStack pose = guiGraphics.pose();
//        pose.pushPose();
        pose.pushMatrix();
        pose.scale(scale, scale);

        guiGraphics.drawString(font, selectedBlocksLabel, centerWidth + xOffset, centerHeight + yOffset, 0xFFFFFFFF);
        guiGraphics.drawString(
                font,
                LiteminerClient.shapes.getCurrentItem().toString(),
                centerWidth + xOffset,
                centerHeight + yOffset + lineHeight,
                0xFFFFFFFF
        );

        pose.popMatrix();
    }

    public static InteractionResult onMouseScroll(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (LiteminerClient.isVeinMining()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (scrollY != 0) {
                if (scrollY > 0) {
                    LiteminerClient.shapes.previousItem();
                } else if (scrollY < 0) {
                    LiteminerClient.shapes.nextItem();
                }
                LiteminerNetwork.sendToServer(new C2SVeinmineKeybindChange(
                        LiteminerClient.isVeinMining(),
                        LiteminerClient.shapes.getCurrentIndex()
                ));
            }
            if (!LiteminerClient.CONFIG.showHUD.get()) {
                assert minecraft.player != null;
                FeedbackHelper.actionBarMessage(
                        minecraft.player,
                        Component.translatable(
                                "hud.liteminer.changed_shape",
                                LiteminerClient.shapes.getCurrentItem().toString()
                        )
                );
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }
}
