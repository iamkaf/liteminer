package com.iamkaf.liteminer.compat;

import com.iamkaf.amber.api.event.v1.events.common.client.RenderEvents;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Block outline rendering. The camera accessor changed in 1.21.6, and the line render API in 1.21.5, 1.21.11,
 * and 26.2.
 */
public final class BlockOutlines {
    private BlockOutlines() {
    }

    /** A line between two points, relative to the pose the renderer draws it in. */
    public record Line(double x1, double y1, double z1, double x2, double y2, double z2) {
    }

    /** Draws see-through lines, then solid ones, with the pose already placed. */
    @FunctionalInterface
    public interface LineSink {
        void draw(List<Line> lines, int seeThroughColor, int solidColor);
    }

    @FunctionalInterface
    public interface Renderer {
        InteractionResult render(Vec3 cameraPosition, PoseStack poseStack, LineSink lines);
    }

    public static void register(Renderer renderer) {
        RenderEvents.BLOCK_OUTLINE_RENDER.register((camera, output, poseStack, hitResult, pos, state) ->
                renderer.render(cameraPosition(camera), poseStack, (lines, seeThroughColor, solidColor) -> {
                    //? if >=1.21.11 {
                    ModernLines.draw(output, poseStack, lines, seeThroughColor, solidColor);
                    //?} else if >=1.21.5 {
                    /*PipelineLines.draw(output, poseStack, lines, seeThroughColor, solidColor);
                    *///?} else {
                    /*ClassicLines.draw(output, poseStack, lines, seeThroughColor, solidColor);
                    *///?}
                }));
    }

    private static Vec3 cameraPosition(Camera camera) {
        //? if >=1.21.6 {
        return camera.position();
        //?} else {
        /*return camera.getPosition();
        *///?}
    }
}
