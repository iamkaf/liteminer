package com.iamkaf.liteminer.compat;

//? if >=1.21.11 {
import com.iamkaf.liteminer.compat.BlockOutlines.Line;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.joml.Vector3f;
//? if >=26.3 {
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.oit.OitPipelineSet;
//?} else if >=26.2 {
/*import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.OutputTarget;
*///?} else if >=26.1 {
/*import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.OutputTarget;
*///?} else {
/*import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.OutputTarget;
*///?}

import java.util.List;

/** Highlight lines on Minecraft 1.21.11 and newer, where render types are built from a pipeline and a setup. */
final class ModernLines {
    private static final RenderType LINES_NORMAL = RenderTypes.lines();
    private static final RenderType LINES_TRANSLUCENT_NO_DEPTH_TEST = createLinesTranslucentNoDepthTestRenderType();

    private ModernLines() {
    }

    private static RenderType createLinesTranslucentNoDepthTestRenderType() {
        //? if >=26.3 {
        // OIT passes supply their own color targets and depth state.
        RenderPipeline.Snippet snippet = RenderPipeline.builder()
                .withBindGroupLayout(BindGroupLayouts.GLOBALS)
                .withBindGroupLayout(BindGroupLayouts.PROJECTION)
                .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                .withBindGroupLayout(BindGroupLayouts.FOG)
                .withVertexShader("core/rendertype_lines")
                .withFragmentShader("core/rendertype_lines")
                .withCull(false)
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
                .withPrimitiveTopology(PrimitiveTopology.LINES)
                .buildSnippet();

        RenderPipeline pipeline = RenderPipeline.builder(snippet)
                .withLocation("pipeline/lines_translucent_no_depth")
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                .build();
        IrisCompat.assignLinesPipeline(pipeline);

        RenderSetup setup = RenderSetup.builder(pipeline)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .setOitPipelines(OitPipelineSet.builder("lines_translucent_no_depth", RenderPipeline.builder(snippet))
                        .withoutDepthTest()
                        .build())
                .createRenderSetup();
        return RenderType.create("lines_translucent_no_depth_test", setup);
        //?} else if >=26.2 {
        /*RenderPipeline.Snippet snippet = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                .withVertexShader("core/rendertype_lines")
                .withFragmentShader("core/rendertype_lines")
                .withCull(false)
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
                .withPrimitiveTopology(PrimitiveTopology.LINES)
                .buildSnippet();

        RenderPipeline pipeline = RenderPipeline.builder(snippet)
                .withLocation("pipeline/lines_translucent_no_depth")
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                .build();
        IrisCompat.assignLinesPipeline(pipeline);

        RenderSetup setup = RenderSetup.builder(pipeline)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                .createRenderSetup();
        return RenderType.create("lines_translucent_no_depth_test", setup);
        *///?} else if >=26.1 {
        /*RenderPipeline.Snippet snippet = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
                .withVertexShader("core/rendertype_lines")
                .withFragmentShader("core/rendertype_lines")
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withCull(false)
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, VertexFormat.Mode.LINES)
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                .buildSnippet();

        RenderPipeline pipeline = RenderPipeline.builder(snippet)
                .withLocation("pipeline/lines_translucent_no_depth")
                .build();
        IrisCompat.assignLinesPipeline(pipeline);

        RenderSetup setup = RenderSetup.builder(pipeline)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                .createRenderSetup();
        return RenderType.create("lines_translucent_no_depth_test", setup);
        *///?} else {
        /*RenderPipeline.Snippet snippet = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
                .withVertexShader("core/rendertype_lines")
                .withFragmentShader("core/rendertype_lines")
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, VertexFormat.Mode.LINES)
                .buildSnippet();

        RenderPipeline pipeline = RenderPipeline.builder(snippet)
                .withLocation("pipeline/lines_translucent_no_depth")
                .build();
        IrisCompat.assignLinesPipeline(pipeline);

        RenderSetup setup = RenderSetup.builder(pipeline)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                .createRenderSetup();
        return RenderType.create("lines_translucent_no_depth_test", setup);
        *///?}
    }

    //? if >=26.2 {
    /** Draws the see-through lines first, then the solid ones, with the pose already placed at the selection. */
    static void draw(SubmitNodeCollector output, PoseStack poseStack, List<Line> lines, int seeThroughColor,
            int solidColor) {
        float width = Minecraft.getInstance().getWindow().getAppropriateLineWidth();
        submit(output, poseStack, LINES_TRANSLUCENT_NO_DEPTH_TEST, lines, seeThroughColor, width);
        submit(output, poseStack, LINES_NORMAL, lines, solidColor, width);
    }

    private static void submit(SubmitNodeCollector output, PoseStack poseStack, RenderType renderType,
            List<Line> lines, int color, float width) {
        output.submitCustomGeometry(poseStack, renderType, (pose, builder) -> write(pose, builder, lines, color, width));
    }
    //?} else {
    /*/^* Draws the see-through lines first, then the solid ones, with the pose already placed at the selection. ^/
    static void draw(MultiBufferSource output, PoseStack poseStack, List<Line> lines, int seeThroughColor,
            int solidColor) {
        float width = Minecraft.getInstance().getWindow().getAppropriateLineWidth();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        write(poseStack.last(), buffers.getBuffer(LINES_TRANSLUCENT_NO_DEPTH_TEST), lines, seeThroughColor, width);
        write(poseStack.last(), buffers.getBuffer(LINES_NORMAL), lines, solidColor, width);
        buffers.endBatch(LINES_TRANSLUCENT_NO_DEPTH_TEST);
        buffers.endBatch(LINES_NORMAL);
    }
    *///?}

    private static void write(PoseStack.Pose pose, VertexConsumer builder, List<Line> lines, int color, float width) {
        for (Line line : lines) {
            Vector3f normal = new Vector3f((float) (line.x2() - line.x1()), (float) (line.y2() - line.y1()),
                    (float) (line.z2() - line.z1())).normalize();
            builder.addVertex(pose, (float) line.x1(), (float) line.y1(), (float) line.z1())
                    .setColor(color).setNormal(pose, normal).setLineWidth(width);
            builder.addVertex(pose, (float) line.x2(), (float) line.y2(), (float) line.z2())
                    .setColor(color).setNormal(pose, normal).setLineWidth(width);
        }
    }
}
//?}
