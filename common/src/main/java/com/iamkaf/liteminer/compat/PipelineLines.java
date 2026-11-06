package com.iamkaf.liteminer.compat;

//? if >=1.21.5 && <1.21.11 {
/*import com.iamkaf.liteminer.compat.BlockOutlines.Line;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
//? if <1.21.6
//import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.List;
import java.util.OptionalDouble;

/^* Highlight lines on Minecraft 1.21.5 through 1.21.10, where render types wrap a pipeline and a composite state. ^/
final class PipelineLines {
    private static final RenderType LINES_NORMAL = RenderType.lines();
    private static final RenderType LINES_TRANSLUCENT_NO_DEPTH_TEST = createLinesTranslucentNoDepthTestRenderType();

    private PipelineLines() {
    }

    private static RenderType createLinesTranslucentNoDepthTestRenderType() {
        //? if >=1.21.6 {
        RenderPipeline pipeline = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
                .withVertexShader("core/rendertype_lines")
                .withFragmentShader("core/rendertype_lines")
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES)
                .withLocation("pipeline/lines_translucent_no_depth")
                .build();
        //?} else {
        /^RenderPipeline pipeline = RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_FOG_SNIPPET)
                .withVertexShader("core/rendertype_lines")
                .withFragmentShader("core/rendertype_lines")
                .withUniform("LineWidth", UniformType.FLOAT)
                .withUniform("ScreenSize", UniformType.VEC2)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES)
                .withLocation("pipeline/lines_translucent_no_depth")
                .build();
        ^///?}

        IrisCompat.assignLinesPipeline(pipeline);

        return RenderType.create(
                "lines_translucent_no_depth_test",
                1536,
                pipeline,
                RenderType.CompositeState.builder()
                        .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
                        .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                        .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                        .createCompositeState(false)
        );
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
            builder.addVertex(pose, (float) line.x1(), (float) line.y1(), (float) line.z1())
                    .setColor(red, green, blue, alpha).setNormal(pose, nx, ny, nz);
            builder.addVertex(pose, (float) line.x2(), (float) line.y2(), (float) line.z2())
                    .setColor(red, green, blue, alpha).setNormal(pose, nx, ny, nz);
        }
    }
}
*///?}
