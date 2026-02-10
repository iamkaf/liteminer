package com.iamkaf.liteminer.rendering;

import com.iamkaf.amber.api.aabb.BoundingBoxMerger;
import com.iamkaf.liteminer.Liteminer;
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
import java.util.Objects;

public class BlockHighlightRenderer {
    private static final RenderType LINES_NORMAL = RenderTypes.lines();

    // Cache for expensive operations
    private static class HighlightCache {
        BlockPos lastOrigin;
        int lastShapeIndex;
        HashSet<BlockPos> cachedBlocks;
        VoxelShape cachedCombinedShape;
        long lastUpdateTime;
        int frameCounter = 0;

        boolean isValid(BlockPos origin, int shapeIndex, int blockLimit) {
            // Adaptive cache timeout based on block limit
            // Higher limits = more expensive calculations = longer cache
            long cacheTimeout = blockLimit >= 512 ? 500 :  // 500ms for very large limits
                               blockLimit >= 256 ? 300 :   // 300ms for large limits
                               blockLimit >= 128 ? 200 :   // 200ms for medium-large limits
                               100;                         // 100ms for normal limits

            return Objects.equals(lastOrigin, origin) &&
                   lastShapeIndex == shapeIndex &&
                   (System.currentTimeMillis() - lastUpdateTime) < cacheTimeout;
        }

        boolean shouldUpdateThisFrame(int blockLimit) {
            // For high block limits, skip frames to reduce overhead
            // Only update every N frames based on block count
            frameCounter++;

            int frameSkip = blockLimit >= 512 ? 4 :  // Update every 4th frame for huge limits
                           blockLimit >= 256 ? 3 :   // Update every 3rd frame for very large limits
                           blockLimit >= 128 ? 2 :   // Update every 2nd frame for medium-large limits
                           1;                         // Update every frame for normal limits

            return (frameCounter % frameSkip) == 0;
        }

        void update(BlockPos origin, int shapeIndex, HashSet<BlockPos> blocks, VoxelShape shape) {
            this.lastOrigin = origin;
            this.lastShapeIndex = shapeIndex;
            this.cachedBlocks = blocks;
            this.cachedCombinedShape = shape;
            this.lastUpdateTime = System.currentTimeMillis();
        }

        void invalidate() {
            this.lastOrigin = null;
            this.cachedBlocks = null;
            this.cachedCombinedShape = null;
        }
    }

    private static final HighlightCache cache = new HighlightCache();

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
            cache.invalidate();
            return InteractionResult.PASS;
        }

        if (!LiteminerClient.isVeinMining()) {
            cache.invalidate();
            return InteractionResult.PASS;
        }

        if (!LiteminerClient.CONFIG.showHighlights.get()) {
            cache.invalidate();
            return InteractionResult.PASS;
        }

        HitResult result = mc.hitResult;
        if (result == null || result.getType() != HitResult.Type.BLOCK) {
            cache.invalidate();
            return InteractionResult.PASS;
        }

        Walker walker = LiteminerClient.shapes.getCurrentItem();
        int currentShapeIndex = LiteminerClient.shapes.getCurrentIndex();
        int blockLimit = Liteminer.CONFIG.blockBreakLimit.get();

        BlockPos origin = ShapelessWalker.raytraceBlock(level, player);

        // Check cache validity
        HashSet<BlockPos> blocksToHighlight;
        VoxelShape combinedShape;

        if (cache.isValid(origin, currentShapeIndex, blockLimit) && cache.shouldUpdateThisFrame(blockLimit)) {
            // Use cached results
            blocksToHighlight = cache.cachedBlocks;
            combinedShape = cache.cachedCombinedShape;
        } else if (cache.isValid(origin, currentShapeIndex, blockLimit)) {
            // Cache is valid but we're skipping this frame for performance
            blocksToHighlight = cache.cachedBlocks;
            combinedShape = cache.cachedCombinedShape;
        } else {
            // Recalculate and cache
            blocksToHighlight = walker.walk(level, player, ShapelessWalker.raytrace(level, player).getBlockPos());

            if (blocksToHighlight.isEmpty()) {
                cache.invalidate();
                LiteminerClient.selectedBlocks = blocksToHighlight;
                return InteractionResult.PASS;
            }

            // Adaptive rendering strategy based on block count
            if (blockLimit >= 128 && blocksToHighlight.size() >= 128) {
                // For high block limits, skip expensive BoundingBoxMerger
                // Render individual block AABBs - much faster for large counts
                combinedShape = createSimplifiedShape(blocksToHighlight, origin);
            } else {
                // For normal block limits, use expensive but prettier merged shapes
                Collection<VoxelShape> shapes = new HashSet<>();
                for (AABB aabb : BoundingBoxMerger.merge(blocksToHighlight.stream().toList(), origin)) {
                    shapes.add(Shapes.create(aabb.inflate(0.005d)));
                }
                combinedShape = orShapes(shapes);
            }

            cache.update(origin, currentShapeIndex, blocksToHighlight, combinedShape);
        }

        LiteminerClient.selectedBlocks = blocksToHighlight;

        Camera renderInfo = mc.gameRenderer.getMainCamera();
        Vec3 projectedView = renderInfo.position();
        assert poseStack != null;
        poseStack.pushPose();
        poseStack.translate(
                origin.getX() - projectedView.x,
                origin.getY() - projectedView.y,
                origin.getZ() - projectedView.z
        );

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float lineWidth = mc.getWindow().getAppropriateLineWidth();

        // Render see-through translucent lines with NO_DEPTH_TEST so they show through blocks
        VertexConsumer translucentBuilder = buffers.getBuffer(LINES_TRANSLUCENT_NO_DEPTH_TEST);
        int translucentColor = LiteminerClient.CONFIG.highlightSeeThroughLineColor.get().argb(0x4B);
        ShapeRenderer.renderShape(poseStack, translucentBuilder, combinedShape,
                0.0, 0.0, 0.0, translucentColor, lineWidth);

        // Render foreground opaque lines that respect depth testing
        VertexConsumer opaqueBuilder = buffers.getBuffer(LINES_NORMAL);
        int opaqueColor = LiteminerClient.CONFIG.highlightForegroundLineColor.get().argb(0xFF);
        ShapeRenderer.renderShape(poseStack, opaqueBuilder, combinedShape,
                0.0, 0.0, 0.0, opaqueColor, lineWidth);

        buffers.endBatch(LINES_TRANSLUCENT_NO_DEPTH_TEST);
        buffers.endBatch(LINES_NORMAL);

        poseStack.popPose();
        return InteractionResult.PASS;
    }

    /**
     * Creates a simplified shape for high block counts (>=128).
     * Skips expensive BoundingBoxMerger and directly combines individual block AABBs.
     * This is much faster for large block counts at the cost of visual fidelity.
     */
    private static VoxelShape createSimplifiedShape(HashSet<BlockPos> blocks, BlockPos origin) {
        VoxelShape combinedShape = Shapes.empty();

        // For very large counts, use even more aggressive optimization
        int blockCount = blocks.size();
        boolean useAggressiveOptimization = blockCount >= 256;

        if (useAggressiveOptimization) {
            // Skip inflation and use raw block AABBs for maximum performance
            for (BlockPos pos : blocks) {
                BlockPos relative = pos.subtract(origin);
                AABB box = new AABB(relative.getX(), relative.getY(), relative.getZ(),
                                   relative.getX() + 1, relative.getY() + 1, relative.getZ() + 1);
                combinedShape = Shapes.join(combinedShape, Shapes.create(box), BooleanOp.OR);
            }
        } else {
            // Use slight inflation for better visuals while still being faster than merging
            for (BlockPos pos : blocks) {
                BlockPos relative = pos.subtract(origin);
                AABB box = new AABB(relative.getX(), relative.getY(), relative.getZ(),
                                   relative.getX() + 1, relative.getY() + 1, relative.getZ() + 1)
                                   .inflate(0.002d);  // Reduced inflation for performance
                combinedShape = Shapes.join(combinedShape, Shapes.create(box), BooleanOp.OR);
            }
        }

        return combinedShape.optimize();
    }

    static VoxelShape orShapes(Collection<VoxelShape> shapes) {
        VoxelShape combinedShape = Shapes.empty();
        for (VoxelShape shape : shapes) {
            // Use optimized version - it's faster than joinUnoptimized
            combinedShape = Shapes.join(combinedShape, shape, BooleanOp.OR);
        }
        return combinedShape.optimize();
    }
}
