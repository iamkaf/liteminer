package com.iamkaf.liteminer.rendering;

import com.iamkaf.amber.api.functions.v1.WorldFunctions;
import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.LiteminerClient;
import com.iamkaf.liteminer.api.shape.LiteminerShape;
import com.iamkaf.liteminer.config.LineColor;
import com.iamkaf.liteminer.shapes.ShapelessWalker;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
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
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class BlockHighlightRenderer {
    private static final long HIGHLIGHT_COLOR_PERIOD_MS = 1_200L;
    private static final RenderType LINES_NORMAL = RenderTypes.lines();
    private static final RenderType LINES_TRANSLUCENT_NO_DEPTH_TEST = createLinesTranslucentNoDepthTestRenderType();

    // Cache for expensive operations
    private static class HighlightCache {
        BlockPos lastOrigin;
        int lastShapeIndex;
        HashSet<BlockPos> cachedBlocks;
        VoxelShape cachedCombinedShape;
        List<Line> cachedLines;
        long lastUpdateTime;
        BlockPos lastCameraBlock;
        int lastYawBucket;
        int lastPitchBucket;
        int frameCounter = 0;

        boolean isValid(BlockPos origin, int shapeIndex, int blockLimit, Camera camera) {
            // Adaptive cache timeout based on block limit
            // Higher limits = more expensive calculations = longer cache
            long cacheTimeout = blockLimit >= 512 ? 500 :  // 500ms for very large limits
                               blockLimit >= 256 ? 300 :   // 300ms for large limits
                               blockLimit >= 128 ? 200 :   // 200ms for medium-large limits
                               100;                         // 100ms for normal limits

            return Objects.equals(lastOrigin, origin) &&
                   lastShapeIndex == shapeIndex &&
                   // Render geometry is camera-culled, so camera movement has to invalidate it.
                   Objects.equals(lastCameraBlock, camera.blockPosition()) &&
                   lastYawBucket == rotationBucket(camera.yaw()) &&
                   lastPitchBucket == rotationBucket(camera.xRot()) &&
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

        void update(BlockPos origin, int shapeIndex, Camera camera, HashSet<BlockPos> blocks, VoxelShape shape, List<Line> lines) {
            this.lastOrigin = origin;
            this.lastShapeIndex = shapeIndex;
            this.lastCameraBlock = camera.blockPosition();
            this.lastYawBucket = rotationBucket(camera.yaw());
            this.lastPitchBucket = rotationBucket(camera.xRot());
            this.cachedBlocks = blocks;
            this.cachedCombinedShape = shape;
            this.cachedLines = lines;
            this.lastUpdateTime = System.currentTimeMillis();
        }

        void invalidate() {
            this.lastOrigin = null;
            this.cachedBlocks = null;
            this.cachedCombinedShape = null;
            this.cachedLines = null;
            this.lastCameraBlock = null;
        }
    }

    private static final HighlightCache cache = new HighlightCache();

    private static RenderType createLinesTranslucentNoDepthTestRenderType() {
        RenderPipeline.Snippet snippet = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                .withVertexShader("core/rendertype_lines")
                .withFragmentShader("core/rendertype_lines")
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withCull(false)
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
                .withPrimitiveTopology(PrimitiveTopology.LINES)
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                .buildSnippet();

        RenderPipeline pipeline = RenderPipeline.builder(snippet)
                .withLocation("pipeline/lines_translucent_no_depth")
                .build();

        RenderSetup setup = RenderSetup.builder(pipeline)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                .createRenderSetup();

        return RenderType.create("lines_translucent_no_depth_test", setup);
    }

    public static InteractionResult renderLiteminerHighlight(Camera camera, SubmitNodeCollector submitNodeCollector, PoseStack poseStack, BlockHitResult blockHitResult, BlockPos blockPos, BlockState blockState) {
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

        LiteminerShape shape = LiteminerClient.shapes.getCurrentItem();
        int currentShapeIndex = LiteminerClient.shapes.getCurrentIndex();
        int blockLimit = Liteminer.CONFIG.blockBreakLimit.get();

        BlockPos origin = ShapelessWalker.raytraceBlock(level, player);

        // Check cache validity
        HashSet<BlockPos> blocksToHighlight;
        VoxelShape combinedShape;
        List<Line> linesToRender;

        if (cache.isValid(origin, currentShapeIndex, blockLimit, camera) && cache.shouldUpdateThisFrame(blockLimit)) {
            // Use cached results
            blocksToHighlight = cache.cachedBlocks;
            combinedShape = cache.cachedCombinedShape;
            linesToRender = cache.cachedLines;
        } else if (cache.isValid(origin, currentShapeIndex, blockLimit, camera)) {
            // Cache is valid but we're skipping this frame for performance
            blocksToHighlight = cache.cachedBlocks;
            combinedShape = cache.cachedCombinedShape;
            linesToRender = cache.cachedLines;
        } else {
            // Recalculate and cache
            blocksToHighlight = shape.walk(level, player, ShapelessWalker.raytrace(level, player).getBlockPos());

            if (blocksToHighlight.isEmpty()) {
                cache.invalidate();
                LiteminerClient.selectedBlocks = blocksToHighlight;
                return InteractionResult.PASS;
            }

            HashSet<BlockPos> renderBlocks = cullBlocksOutsideCamera(blocksToHighlight, camera, mc);
            if (renderBlocks.isEmpty()) {
                renderBlocks = blocksToHighlight;
            }

            if (renderBlocks.size() >= 64) {
                // Avoid VoxelShape unions for dense selections; plant and leaf shapes are especially expensive.
                linesToRender = createBoundaryLines(renderBlocks, origin);
                combinedShape = null;
            } else {
                // For normal block limits, use expensive but prettier merged shapes
                Collection<VoxelShape> shapes = new HashSet<>();
                for (AABB aabb : WorldFunctions.mergeBoundingBoxes(renderBlocks, origin)) {
                    shapes.add(Shapes.create(aabb.inflate(0.005d)));
                }
                combinedShape = orShapes(shapes);
                linesToRender = null;
            }

            cache.update(origin, currentShapeIndex, camera, blocksToHighlight, combinedShape, linesToRender);
        }

        LiteminerClient.selectedBlocks = blocksToHighlight;

        Camera renderInfo = mc.gameRenderer.mainCamera();
        Vec3 projectedView = renderInfo.position();
        assert poseStack != null;
        poseStack.pushPose();
        poseStack.translate(
                origin.getX() - projectedView.x,
                origin.getY() - projectedView.y,
                origin.getZ() - projectedView.z
        );

        float lineWidth = mc.getWindow().getAppropriateLineWidth();

        // Render see-through translucent lines with NO_DEPTH_TEST so they show through blocks
        int translucentColor = highlightColor(LiteminerClient.CONFIG.highlightSeeThroughLineColor.get(), 0x4B);
        submitHighlight(submitNodeCollector, poseStack, LINES_TRANSLUCENT_NO_DEPTH_TEST, combinedShape, linesToRender, translucentColor, lineWidth, true);

        // Render foreground opaque lines that respect depth testing
        int opaqueColor = highlightColor(LiteminerClient.CONFIG.highlightForegroundLineColor.get(), 0xFF);
        submitHighlight(submitNodeCollector, poseStack, LINES_NORMAL, combinedShape, linesToRender, opaqueColor, lineWidth, false);

        poseStack.popPose();
        return InteractionResult.PASS;
    }

    private static int highlightColor(LineColor color, int alpha) {
        if (!LiteminerClient.CONFIG.highlightColorTransition.get()) {
            return color.argb(alpha);
        }

        return color.transitioningArgb(alpha, System.currentTimeMillis(), HIGHLIGHT_COLOR_PERIOD_MS);
    }

    private static void submitHighlight(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, RenderType renderType,
            VoxelShape shape, List<Line> lines, int color, float width, boolean afterTerrain) {
        if (lines != null) {
            submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, builder) -> {
                for (Line line : lines) {
                    addLine(pose, builder, line.x1, line.y1, line.z1, line.x2, line.y2, line.z2, color, width);
                }
            });
            return;
        }

        submitNodeCollector.submitShapeOutline(poseStack, shape, renderType, color, width, afterTerrain);
    }

    private static void addLine(PoseStack.Pose pose, VertexConsumer builder, double x1, double y1, double z1,
            double x2, double y2, double z2, int color, float width) {
        Vector3f normal = new Vector3f((float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1)).normalize();
        builder.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(color).setNormal(pose, normal).setLineWidth(width);
        builder.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(color).setNormal(pose, normal).setLineWidth(width);
    }

    private static List<Line> createBoundaryLines(HashSet<BlockPos> blocks, BlockPos origin) {
        HashSet<Line> lines = new HashSet<>();

        for (BlockPos pos : blocks) {
            int x = pos.getX() - origin.getX();
            int y = pos.getY() - origin.getY();
            int z = pos.getZ() - origin.getZ();

            boolean west = isFaceExposed(blocks, pos, -1, 0, 0);
            boolean east = isFaceExposed(blocks, pos, 1, 0, 0);
            boolean down = isFaceExposed(blocks, pos, 0, -1, 0);
            boolean up = isFaceExposed(blocks, pos, 0, 1, 0);
            boolean north = isFaceExposed(blocks, pos, 0, 0, -1);
            boolean south = isFaceExposed(blocks, pos, 0, 0, 1);

            if (down && north) lines.add(new Line(x, y, z, x + 1, y, z));
            if (up && north) lines.add(new Line(x, y + 1, z, x + 1, y + 1, z));
            if (down && south) lines.add(new Line(x, y, z + 1, x + 1, y, z + 1));
            if (up && south) lines.add(new Line(x, y + 1, z + 1, x + 1, y + 1, z + 1));

            if (west && north) lines.add(new Line(x, y, z, x, y + 1, z));
            if (east && north) lines.add(new Line(x + 1, y, z, x + 1, y + 1, z));
            if (west && south) lines.add(new Line(x, y, z + 1, x, y + 1, z + 1));
            if (east && south) lines.add(new Line(x + 1, y, z + 1, x + 1, y + 1, z + 1));

            if (west && down) lines.add(new Line(x, y, z, x, y, z + 1));
            if (east && down) lines.add(new Line(x + 1, y, z, x + 1, y, z + 1));
            if (west && up) lines.add(new Line(x, y + 1, z, x, y + 1, z + 1));
            if (east && up) lines.add(new Line(x + 1, y + 1, z, x + 1, y + 1, z + 1));
        }

        return new ArrayList<>(lines);
    }

    private static HashSet<BlockPos> cullBlocksOutsideCamera(HashSet<BlockPos> blocks, Camera camera, Minecraft mc) {
        // Culling only affects the rendered outline. selectedBlocks keeps the full set for HUD/gameplay.
        if (blocks.size() <= 16) {
            return blocks;
        }

        Vec3 cameraPos = camera.position();
        Vector3fc forward = camera.forwardVector();
        Vector3fc left = camera.leftVector();
        Vector3fc up = camera.upVector();

        double aspect = (double) mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
        double verticalTan = Math.tan(Math.toRadians(camera.getFov()) * 0.5d);
        double horizontalTan = verticalTan * aspect;
        double margin = 1.75d;

        HashSet<BlockPos> visible = new HashSet<>();
        for (BlockPos pos : blocks) {
            double x = pos.getX() + 0.5d - cameraPos.x;
            double y = pos.getY() + 0.5d - cameraPos.y;
            double z = pos.getZ() + 0.5d - cameraPos.z;

            double depth = dot(x, y, z, forward);
            if (depth < -margin) {
                continue;
            }

            double horizontal = Math.abs(dot(x, y, z, left));
            double vertical = Math.abs(dot(x, y, z, up));
            double safeDepth = Math.max(depth, 0.0d);
            if (horizontal <= safeDepth * horizontalTan + margin && vertical <= safeDepth * verticalTan + margin) {
                visible.add(pos);
            }
        }

        return visible;
    }

    private static double dot(double x, double y, double z, Vector3fc vector) {
        return x * vector.x() + y * vector.y() + z * vector.z();
    }

    private static int rotationBucket(float rotation) {
        return Math.round(rotation * 2.0f);
    }

    private static boolean isFaceExposed(HashSet<BlockPos> blocks, BlockPos pos, int x, int y, int z) {
        return !blocks.contains(new BlockPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z));
    }

    private record Line(int x1, int y1, int z1, int x2, int y2, int z2) {}

    static VoxelShape orShapes(Collection<VoxelShape> shapes) {
        VoxelShape combinedShape = Shapes.empty();
        for (VoxelShape shape : shapes) {
            // Use optimized version - it's faster than joinUnoptimized
            combinedShape = Shapes.join(combinedShape, shape, BooleanOp.OR);
        }
        return combinedShape.optimize();
    }
}
