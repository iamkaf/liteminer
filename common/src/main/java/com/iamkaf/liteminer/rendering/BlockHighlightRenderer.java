package com.iamkaf.liteminer.rendering;

import com.iamkaf.amber.api.aabb.BoundingBoxMerger;
import com.iamkaf.liteminer.LiteminerClient;
import com.iamkaf.liteminer.walker.Walker;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
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

import static net.minecraft.client.renderer.RenderStateShard.*;

public class BlockHighlightRenderer {
    private static final RenderType LINES_NORMAL = RenderType.create(
            "liteminer_lines_normal",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.DEBUG_LINES,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setLineState(new LineStateShard(OptionalDouble.empty()))
                    .setLayeringState(NO_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(CULL)
                    .createCompositeState(false)
    );
    private static final RenderType LINES_TRANSPARENT = RenderType.create(
            "liteminer_lines_transparent",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.DEBUG_LINES,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setLineState(new LineStateShard(OptionalDouble.empty()))
                    .setLayeringState(NO_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setCullState(CULL)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .createCompositeState(false)
    );

    public static boolean renderLiteminerHighlight(PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) {
            return true;
        }

        if (!LiteminerClient.KEY_MAPPING.isDown()) {
            return true;
        }

        HitResult result = mc.hitResult;
        if (result == null || result.getType() != HitResult.Type.BLOCK) {
            return true;
        }

        Walker walker = new Walker();

        BlockPos origin = Walker.raytraceBlock(level, player);

        HashSet<BlockPos> blocksToHighlight =
                walker.walk(level, player, Walker.raytrace(level, player).getBlockPos());
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

        VertexConsumer vertexBuilder = buffers.getBuffer(LINES_NORMAL);

        orShapes(shapes).forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            vertexBuilder.addVertex(matrix, (float) x1, (float) y1, (float) z1).setColor(1f, 1f, 1f, 1f);
            vertexBuilder.addVertex(matrix, (float) x2, (float) y2, (float) z2).setColor(1f, 1f, 1f, 1f);
        });
        buffers.endBatch(LINES_NORMAL);

        VertexConsumer vertexBuilder2 = buffers.getBuffer(LINES_TRANSPARENT);

        orShapes(shapes).forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            float q = (float) (x2 - x1);
            float r = (float) (y2 - y1);
            float s = (float) (z2 - z1);
            float t = Mth.sqrt(q * q + r * r + s * s);
            q /= t;
            r /= t;
            s /= t;
            vertexBuilder2.addVertex(matrix, (float) x1, (float) y1, (float) z1)
                    .setColor(10, 206, 245, 180)
                    .setNormal(poseStack.last(), q, r, s);
            vertexBuilder2.addVertex(matrix, (float) x2, (float) y2, (float) z2)
                    .setColor(10, 206, 245, 180)
                    .setNormal(poseStack.last(), q, r, s);
        });
        buffers.endBatch(LINES_TRANSPARENT);


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
