package com.iamkaf.liteminer.rendering;

import com.iamkaf.amber.api.player.FeedbackHelper;
import com.iamkaf.liteminer.Liteminer;
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

        int selectedBlockCount = LiteminerClient.selectedBlocks.size();

        if (selectedBlockCount == 0) {
            return;
        }

        if (!LiteminerClient.isVeinMining() || !LiteminerClient.isTargetingABlock()) {
            return;
        }

        Font font = LiteminerClient.mc.font;

        int lineHeight = 10;

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        int centerWidth = width / 2;
        int centerHeight = height / 2;

        int xOffset = 5;
        int yOffset = -10;

        String selectedBlocksLabel =
                Component.translatable("hud.liteminer.selected_blocks", selectedBlockCount).getString();

        font.width(selectedBlocksLabel);

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
        guiGraphics.bufferSource().endBatch();
    }

    public static EventResult onMouseScroll(Minecraft minecraft, double x, double y) {
        if (LiteminerClient.isVeinMining()) {
            Liteminer.LOGGER.info("Scroll: {} {}", x, y);
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
                FeedbackHelper.actionBarMessage(
                        minecraft.player,
                        Component.literal(String.format(
                                "Changed Shape [%s]",
                                LiteminerClient.shapes.getCurrentItem().toString()
                        ))
                );
            }
            return EventResult.interruptTrue();
        }

        return EventResult.pass();
    }

    enum HUD_ANCHOR {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER
    }
}
