package com.iamkaf.liteminer.rendering;

import com.iamkaf.amber.api.aabb.ShapeMerger;
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
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.*;

import static net.minecraft.client.renderer.RenderStateShard.*;

public class BlockHighlightRenderer {
    public static final Set<BlockPos> HARVESTED_BLOCKS = new HashSet<>();
    public static final int LIMIT = 64;

    public static final RenderType LINES_NORMAL = RenderType.create("liteminer_lines_normal",
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
    public static final RenderType LINES_TRANSPARENT = RenderType.create("liteminer_lines_transparent",
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

//        ItemStack tool = player.getMainHandItem();

        if (player.isShiftKeyDown()) {
            return true;
        }

        HitResult result = mc.hitResult;
        if (result == null || result.getType() != HitResult.Type.BLOCK) {
            return true;
        }

        BlockPos origin = Walker.getRayTracedBlockPosition(level, player);

        HashSet<BlockPos> blocksToHighlight = Walker.getBlocksToHighlight(level, player, Walker.getRayTraceResult(level, player).getBlockPos());
        if (blocksToHighlight.isEmpty()) {
            return true;
        }

        Camera renderInfo = mc.gameRenderer.getMainCamera();
        Vec3 projectedView = renderInfo.getPosition();
        assert poseStack != null;
        poseStack.pushPose();
        poseStack.translate(origin.getX() - projectedView.x,
                origin.getY() - projectedView.y,
                origin.getZ() - projectedView.z
        );
        Matrix4f matrix = poseStack.last().pose();

        // TODO: optimize this
        Collection<VoxelShape> shapes = new HashSet<>();
        for (AABB aabb : ShapeMerger.merge(blocksToHighlight.stream()
//                .filter(blockPos -> tool.isCorrectToolForDrops(level.getBlockState(blockPos)))
                .toList(), origin)) {
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
                    .setColor(1f, 1f, 1f, 0.6f)
                    .setNormal(poseStack.last(), q, r, s);
            vertexBuilder2.addVertex(matrix, (float) x2, (float) y2, (float) z2)
                    .setColor(1f, 1f, 1f, 0.6f)
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

    public static class Walker {
        public static final Set<BlockPos> VISITED = new HashSet<>();
        public static final int RANGE = 64;

        protected static HashSet<BlockPos> getBlocksToHighlight(Level level, Player player, BlockPos origin) {
            HashSet<BlockPos> potentialBrokenBlocks = new HashSet<>();

            searchBlocksToCollapse(
                    level,
                    origin,
                    origin,
                    potentialBrokenBlocks,
                    level.getBlockState(origin).getBlock()
            );
            VISITED.clear();

            return potentialBrokenBlocks;
        }

        private static void searchBlocksToCollapse(Level level, BlockPos myPos,
                BlockPos absoluteOrigin, HashSet<BlockPos> blocksToCollapse, Block kind) {
            if (VISITED.size() >= LIMIT) return;
            if (VISITED.contains(myPos)) return;
            if (!level.getBlockState(myPos).is(kind)) return;

            blocksToCollapse.add(myPos);
            VISITED.add(myPos);

            for (var neighborPos : getNeighbors(myPos, absoluteOrigin)) {
                searchBlocksToCollapse(level, neighborPos, absoluteOrigin, blocksToCollapse, kind);
            }
        }

        private static List<BlockPos> getNeighbors(BlockPos myPos, BlockPos absoluteOrigin) {
            List<BlockPos> blocks = new ArrayList<>();

            blocks.add(myPos.above());
            blocks.add(myPos.below());
            blocks.add(myPos.north());
            blocks.add(myPos.south());
            blocks.add(myPos.east());
            blocks.add(myPos.west());
            blocks.add(myPos.north().east());
            blocks.add(myPos.north().west());
            blocks.add(myPos.south().east());
            blocks.add(myPos.south().west());

            blocks.add(myPos.above().north());
            blocks.add(myPos.above().south());
            blocks.add(myPos.above().east());
            blocks.add(myPos.above().west());
            blocks.add(myPos.above().north().east());
            blocks.add(myPos.above().north().west());
            blocks.add(myPos.above().south().east());
            blocks.add(myPos.above().south().west());

            blocks.add(myPos.below().north());
            blocks.add(myPos.below().south());
            blocks.add(myPos.below().east());
            blocks.add(myPos.below().west());
            blocks.add(myPos.below().north().east());
            blocks.add(myPos.below().north().west());
            blocks.add(myPos.below().south().east());
            blocks.add(myPos.below().south().west());

            blocks.sort(Comparator.comparingInt(p -> p.distManhattan(absoluteOrigin)));

            return blocks;
        }

        public static @NotNull BlockHitResult getRayTraceResult(Level level, Player player) {
            Vec3 eyePosition = player.getEyePosition();
            Vec3 rotation = player.getViewVector(1);
            double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
            Vec3 combined = eyePosition.add(rotation.x * reach, rotation.y * reach, rotation.z * reach);

            return level.clip(new ClipContext(eyePosition,
                    combined,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player
            ));
        }

        public static @NotNull BlockPos getRayTracedBlockPosition(Level level, Player player) {
            var rayTraceResult = getRayTraceResult(level, player);

            Direction direction = rayTraceResult.getDirection();

            return switch (direction) {
                case DOWN, NORTH, WEST -> rayTraceResult.getBlockPos();
                case UP -> rayTraceResult.getBlockPos().below();
                case SOUTH -> rayTraceResult.getBlockPos().north();
                case EAST -> rayTraceResult.getBlockPos().west();
            };
        }
    }
}
