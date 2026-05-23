package com.iamkaf.liteminer.rendering;

import com.iamkaf.amber.api.functions.v1.PlayerFunctions;
import com.iamkaf.liteminer.LiteminerClient;
import com.iamkaf.liteminer.api.event.LiteminerClientEvents;
import com.iamkaf.liteminer.api.event.LiteminerHudContext;
import com.iamkaf.liteminer.api.shape.LiteminerShape;
import com.iamkaf.liteminer.networking.C2SVeinmineKeybindChange;
import com.iamkaf.liteminer.networking.LiteminerNetwork;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

public class HUD {
    public static void onRenderHUD(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
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

        float scale = LiteminerClient.CONFIG.hud_scale.get().floatValue();

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        int centerWidth = (int) (width / 2f / scale);
        int centerHeight = (int) (height / 2f / scale);

        Component selectedBlocksLabel = Component.translatable(
                selectedBlockCount > 1 ? "hud.liteminer.selected_blocks" : "hud.liteminer" + ".selected_blocks_singular",
                selectedBlockCount
        );
        LiteminerShape selectedShape = LiteminerClient.shapes.getCurrentItem();
        List<Component> lines = new ArrayList<>();
        lines.add(selectedBlocksLabel);
        lines.add(selectedShape.displayName());

        LiteminerHudContext context = new LiteminerHudContext(selectedBlockCount, selectedShape, lines);
        LiteminerClientEvents.MODIFY_HUD.invoker().modifyHud(context);
        if (!context.visible() || context.lines().isEmpty()) {
            return;
        }

        int xOffset = (int) (context.xOffset() / scale);
        int yOffset = (int) (context.yOffset() / scale);
        int lineHeight = context.lineHeight();

        Matrix3x2fStack pose = guiGraphics.pose();
//        pose.pushPose();
        pose.pushMatrix();
        pose.scale(scale, scale);

        for (int i = 0; i < context.lines().size(); i++) {
            guiGraphics.text(
                    font,
                    context.lines().get(i),
                    centerWidth + xOffset,
                    centerHeight + yOffset + i * lineHeight,
                    context.textColor()
            );
        }

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
                PlayerFunctions.sendActionBar(
                        minecraft.player,
                        Component.translatable(
                                "hud.liteminer.changed_shape",
                                LiteminerClient.shapes.getCurrentItem().displayName()
                        )
                );
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }
}
