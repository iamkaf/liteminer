package com.iamkaf.liteminer.rendering;

import com.iamkaf.amber.api.aabb.BoundingBoxMerger;
import com.iamkaf.liteminer.LiteminerClient;
import com.iamkaf.liteminer.shapes.ShapelessWalker;
import com.iamkaf.liteminer.shapes.Walker;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;

import java.util.Collection;
import java.util.HashSet;
import java.util.OptionalDouble;

public class BlockHighlightRenderer {
    private static final RenderType LINES_NORMAL = RenderType.lines();

    // for this to work I had to add some access wideners
    private static final RenderPipeline TRANSPARENT_LINES_PIPELINE =
            RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                    .withVertexShader("core/rendertype_lines")
                    .withFragmentShader("core/rendertype_lines")
                    .withUniform("LineWidth", UniformType.UNIFORM_BUFFER)
                    .withUniform("ScreenSize", UniformType.UNIFORM_BUFFER)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINES)
                    .withLocation("pipeline/transparent_lines")
                    .build());

    private static final RenderType LINES_TRANSPARENT = RenderType.create(
            "lines",
            1536, TRANSPARENT_LINES_PIPELINE,
            RenderType.CompositeState.builder()
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
                    .setLayeringState(RenderType.NO_LAYERING)
                    .setOutputState(RenderType.ITEM_ENTITY_TARGET)
                    .createCompositeState(false)
    );

    public static boolean renderLiteminerHighlight(PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) {
            return true;
        }

        if (!LiteminerClient.isVeinMining()) {
            return true;
        }

        HitResult result = mc.hitResult;
        if (result == null || result.getType() != HitResult.Type.BLOCK) {
            return true;
        }

        Walker walker = LiteminerClient.shapes.getCurrentItem();

        BlockPos origin = ShapelessWalker.raytraceBlock(level, player);

        HashSet<BlockPos> blocksToHighlight =
                walker.walk(level, player, ShapelessWalker.raytrace(level, player).getBlockPos());
        LiteminerClient.selectedBlocks = blocksToHighlight;
        if (blocksToHighlight.isEmpty()) {
            return true;
        }

        Camera renderInfo = mc.gameRenderer.getMainCamera();
        Vec3 projectedView = renderInfo.getPosition();
        assert poseStack != null;
        poseStack.pushPose();
        poseStack.translate(
                origin.getX() - projectedView.x,
                origin.getY() - projectedView.y,
                origin.getZ() - projectedView.z
        );
        Matrix4f matrix = poseStack.last().pose();

        Collection<VoxelShape> shapes = new HashSet<>();
        for (AABB aabb : BoundingBoxMerger.merge(blocksToHighlight.stream().toList(), origin)) {
            shapes.add(Shapes.create(aabb.inflate(0.005d)));
        }

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        VertexConsumer vertexBuilder2 = buffers.getBuffer(LINES_TRANSPARENT);

        orShapes(shapes).forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            final double dx = x2 - x1;
            final double dy = y2 - y1;
            final double dz = z2 - z1;

            final double invMag = 1.0 / Math.sqrt(dx * dx + dy * dy + dz * dz);
            final float nx = (float) (dx * invMag);
            final float ny = (float) (dy * invMag);
            final float nz = (float) (dz * invMag);
            PoseStack.Pose pose = poseStack.last();
            vertexBuilder2.addVertex(matrix, (float) x1, (float) y1, (float) z1)
                    .setColor(10, 206, 245, 180)
                    .setNormal(pose, nx, ny, nz);
            vertexBuilder2.addVertex(matrix, (float) x2, (float) y2, (float) z2)
                    .setColor(10, 206, 245, 180)
                    .setNormal(pose, nx, ny, nz);
        });
        buffers.endBatch(LINES_TRANSPARENT);

        VertexConsumer vertexBuilder = buffers.getBuffer(LINES_NORMAL);

        orShapes(shapes).forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            final double dx = x2 - x1;
            final double dy = y2 - y1;
            final double dz = z2 - z1;

            final double invMag = 1.0 / Math.sqrt(dx * dx + dy * dy + dz * dz);
            final float nx = (float) (dx * invMag);
            final float ny = (float) (dy * invMag);
            final float nz = (float) (dz * invMag);
            PoseStack.Pose pose = poseStack.last();
            vertexBuilder.addVertex(matrix, (float) x1, (float) y1, (float) z1)
                    .setColor(1f, 1f, 1f, 1f)
                    .setNormal(pose, nx, ny, nz);
            vertexBuilder.addVertex(matrix, (float) x2, (float) y2, (float) z2)
                    .setColor(1f, 1f, 1f, 1f)
                    .setNormal(pose, nx, ny, nz);
        });
        buffers.endBatch(LINES_NORMAL);

        poseStack.popPose();
        return false;
    }

    static VoxelShape orShapes(Collection<VoxelShape> shapes) {
        VoxelShape combinedShape = Shapes.empty();
        for (VoxelShape shape : shapes) {
            combinedShape = Shapes.joinUnoptimized(combinedShape, shape, BooleanOp.OR);
        }
        return combinedShape;
    }
}
