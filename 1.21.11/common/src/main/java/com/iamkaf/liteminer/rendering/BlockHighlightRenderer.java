package com.iamkaf.liteminer.rendering;

import com.iamkaf.amber.api.aabb.BoundingBoxMerger;
import com.iamkaf.liteminer.LiteminerClient;
import com.iamkaf.liteminer.shapes.ShapelessWalker;
import com.iamkaf.liteminer.shapes.Walker;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collection;
import java.util.HashSet;

public class BlockHighlightRenderer {
    private static final RenderType LINES_NORMAL = RenderTypes.lines();

    // Custom RenderType with NO_DEPTH_TEST for translucent lines that show through blocks
    public static final RenderType LINES_TRANSLUCENT_NO_DEPTH_TEST = createLinesTranslucentNoDepthTestRenderType();

    // Huge shoutout to ChampionAsh5357
    // https://github.com/neoforged/.github/blob/main/primers/1.21.11/index.md
    private static RenderType createLinesTranslucentNoDepthTestRenderType() {
        // Pipeline definition - creating from scratch with NO_DEPTH_TEST because there are no LINES render types with NO_DEPTH_TEST
        RenderPipeline.Snippet snippet = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
            .withVertexShader("core/rendertype_lines")
            .withFragmentShader("core/rendertype_lines")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, VertexFormat.Mode.LINES)
            .buildSnippet();

        RenderPipeline pipeline = RenderPipeline.builder(snippet)
            .withLocation("pipeline/lines_translucent_no_depth")
            .build();

        RenderSetup setup = RenderSetup.builder(pipeline)
            .useLightmap()
            .createRenderSetup();

        return RenderType.create("lines_translucent_no_depth_test", setup);
    }

    public static InteractionResult renderLiteminerHighlight(Camera camera, MultiBufferSource multiBufferSource, PoseStack poseStack, BlockHitResult blockHitResult, BlockPos blockPos, BlockState blockState) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) {
            return InteractionResult.PASS;
        }

        if (!LiteminerClient.isVeinMining()) {
            return InteractionResult.PASS;
        }

        HitResult result = mc.hitResult;
        if (result == null || result.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }

        Walker walker = LiteminerClient.shapes.getCurrentItem();

        BlockPos origin = ShapelessWalker.raytraceBlock(level, player);

        HashSet<BlockPos> blocksToHighlight =
                walker.walk(level, player, ShapelessWalker.raytrace(level, player).getBlockPos());
        LiteminerClient.selectedBlocks = blocksToHighlight;
        if (blocksToHighlight.isEmpty()) {
            return InteractionResult.PASS;
        }

        Camera renderInfo = mc.gameRenderer.getMainCamera();
        Vec3 projectedView = renderInfo.position();
        assert poseStack != null;
        poseStack.pushPose();
        poseStack.translate(
                origin.getX() - projectedView.x,
                origin.getY() - projectedView.y,
                origin.getZ() - projectedView.z
        );

        Collection<VoxelShape> shapes = new HashSet<>();
        for (AABB aabb : BoundingBoxMerger.merge(blocksToHighlight.stream().toList(), origin)) {
            shapes.add(Shapes.create(aabb.inflate(0.005d)));
        }

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float lineWidth = mc.getWindow().getAppropriateLineWidth();

        VoxelShape combinedShape = orShapes(shapes);

        // Render cyan translucent lines with NO_DEPTH_TEST so they show through blocks
        VertexConsumer translucentBuilder = buffers.getBuffer(LINES_TRANSLUCENT_NO_DEPTH_TEST);
        // ARGB format: A=0x4B (75%), R=0x0A (10), G=0xCE (206), B=0xF5 (245)
        int cyanColor = (0x4B << 24) | (0x0A << 16) | (0xCE << 8) | 0xF5;
        ShapeRenderer.renderShape(poseStack, translucentBuilder, combinedShape,
                0.0, 0.0, 0.0, cyanColor, lineWidth);

        // Render white opaque lines that respect depth testing
        VertexConsumer opaqueBuilder = buffers.getBuffer(LINES_NORMAL);
        // ARGB format: A=0xFF (100%), R=0xFF, G=0xFF, B=0xFF
        int whiteColor = (0xFF << 24) | (0xFF << 16) | (0xFF << 8) | 0xFF;
        ShapeRenderer.renderShape(poseStack, opaqueBuilder, combinedShape,
                0.0, 0.0, 0.0, whiteColor, lineWidth);

        buffers.endBatch(LINES_TRANSLUCENT_NO_DEPTH_TEST);
        buffers.endBatch(LINES_NORMAL);

        poseStack.popPose();
        return InteractionResult.PASS;
    }

    static VoxelShape orShapes(Collection<VoxelShape> shapes) {
        VoxelShape combinedShape = Shapes.empty();
        for (VoxelShape shape : shapes) {
            combinedShape = Shapes.joinUnoptimized(combinedShape, shape, BooleanOp.OR);
        }
        return combinedShape;
    }
}
