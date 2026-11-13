package com.iamkaf.liteminer.compat;

//? if <1.21.5 {
/*import com.iamkaf.liteminer.compat.BlockOutlines.Line;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.List;
import java.util.OptionalDouble;

/^* Highlight lines before Minecraft 1.21.5, where render types are assembled from render state shards. ^/
final class ClassicLines {
    private static final RenderType LINES_NORMAL = RenderType.lines();
    private static final RenderType LINES_TRANSLUCENT_NO_DEPTH_TEST = new TranslucentNoDepthLines();

    // The vanilla lines type, except it ignores the depth buffer so the outline shows through blocks. Its render
    // state shards are protected static members of RenderType's superclass, so the type is a subclass.
    private static final class TranslucentNoDepthLines extends RenderType {
        private static final RenderStateShard[] STATES = {
                RENDERTYPE_LINES_SHADER,
                TRANSLUCENT_TRANSPARENCY,
                NO_DEPTH_TEST,
                NO_CULL,
                VIEW_OFFSET_Z_LAYERING,
                ITEM_ENTITY_TARGET,
                COLOR_WRITE,
                new LineStateShard(OptionalDouble.empty())
        };

        private TranslucentNoDepthLines() {
            super("liteminer_lines_translucent_no_depth_test", DefaultVertexFormat.POSITION_COLOR_NORMAL,
                    VertexFormat.Mode.LINES, 1536, false, false,
                    TranslucentNoDepthLines::setup, TranslucentNoDepthLines::clear);
        }

        private static void setup() {
            for (RenderStateShard state : STATES) {
                state.setupRenderState();
            }
        }

        private static void clear() {
            for (int i = STATES.length - 1; i >= 0; i--) {
                STATES[i].clearRenderState();
            }
        }
    }

    private ClassicLines() {
    }

    /^* Draws the see-through lines first, then the solid ones, with the pose already placed at the selection. ^/
    static void draw(MultiBufferSource output, PoseStack poseStack, List<Line> lines,
            int seeThroughColor, int solidColor) {
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        write(poseStack.last(), buffers.getBuffer(LINES_TRANSLUCENT_NO_DEPTH_TEST), lines, seeThroughColor);
        write(poseStack.last(), buffers.getBuffer(LINES_NORMAL), lines, solidColor);
        buffers.endBatch(LINES_TRANSLUCENT_NO_DEPTH_TEST);
        buffers.endBatch(LINES_NORMAL);
    }

    private static void write(PoseStack.Pose pose, VertexConsumer builder, List<Line> lines,
            int color) {
        int alpha = color >>> 24;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        for (Line line : lines) {
            double dx = line.x2() - line.x1();
            double dy = line.y2() - line.y1();
            double dz = line.z2() - line.z1();
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            float nx = (float) (dx / length);
            float ny = (float) (dy / length);
            float nz = (float) (dz / length);
            //? if >=1.21 {
            builder.addVertex(pose, (float) line.x1(), (float) line.y1(), (float) line.z1())
                    .setColor(red, green, blue, alpha).setNormal(pose, nx, ny, nz);
            builder.addVertex(pose, (float) line.x2(), (float) line.y2(), (float) line.z2())
                    .setColor(red, green, blue, alpha).setNormal(pose, nx, ny, nz);
            //?} else {
            /^builder.vertex(pose.pose(), (float) line.x1(), (float) line.y1(), (float) line.z1())
                    .color(red, green, blue, alpha).normal(pose.normal(), nx, ny, nz).endVertex();
            builder.vertex(pose.pose(), (float) line.x2(), (float) line.y2(), (float) line.z2())
                    .color(red, green, blue, alpha).normal(pose.normal(), nx, ny, nz).endVertex();
            ^///?}
        }
    }
}
*///?}
