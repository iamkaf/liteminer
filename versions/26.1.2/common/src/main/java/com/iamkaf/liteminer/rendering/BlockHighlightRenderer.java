package com.iamkaf.liteminer.rendering;

import com.iamkaf.amber.api.functions.v1.WorldFunctions;
import com.iamkaf.liteminer.LiteminerClient;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BlockHighlightRenderer {
    private static final RenderType LINES_NORMAL = RenderTypes.lines();
    private static final RenderType LINES_TRANSLUCENT_NO_DEPTH_TEST = createLinesTranslucentNoDepthTestRenderType();
    private static final int GRID_EXTERIOR_THRESHOLD = 768;
    private static final double SAMPLE_EPSILON = 0.0001d;

    private static class HighlightCache {
        BlockPos lastOrigin;
        int lastShapeIndex;
        int lastBlockLimit;
        HashSet<BlockPos> cachedBlocks;
        List<Line> cachedLines;
        long lastUpdateTime;

        boolean isValid(BlockPos origin, int shapeIndex, int blockLimit) {
            return Objects.equals(lastOrigin, origin) &&
                    lastShapeIndex == shapeIndex &&
                    lastBlockLimit == blockLimit &&
                    (System.currentTimeMillis() - lastUpdateTime) < cacheTimeoutMillis(blockLimit);
        }

        void update(BlockPos origin, int shapeIndex, int blockLimit, HashSet<BlockPos> blocks, List<Line> lines) {
            this.lastOrigin = origin;
            this.lastShapeIndex = shapeIndex;
            this.lastBlockLimit = blockLimit;
            this.cachedBlocks = blocks;
            this.cachedLines = lines;
            this.lastUpdateTime = System.currentTimeMillis();
        }

        void invalidate() {
            this.lastOrigin = null;
            this.cachedBlocks = null;
            this.cachedLines = null;
        }
    }

    private static final HighlightCache cache = new HighlightCache();

    private static RenderType createLinesTranslucentNoDepthTestRenderType() {
        RenderPipeline.Snippet snippet = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
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

        RenderSetup setup = RenderSetup.builder(pipeline)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                .createRenderSetup();

        return RenderType.create("lines_translucent_no_depth_test", setup);
    }

    public static InteractionResult renderLiteminerHighlight(Camera camera, MultiBufferSource multiBufferSource, PoseStack poseStack, BlockHitResult blockHitResult, BlockPos blockPos, BlockState blockState) {
        Minecraft mc = Minecraft.getInstance();
        LiteminerSelection.Snapshot selection = LiteminerSelection.refresh();
        if (!selection.hasBlocks()) {
            cache.invalidate();
            return InteractionResult.PASS;
        }

        if (!LiteminerClient.CONFIG.showHighlights.get()) {
            cache.invalidate();
            return InteractionResult.PASS;
        }

        List<Line> linesToRender;

        if (cache.isValid(selection.origin(), selection.shapeIndex(), selection.blockLimit())) {
            linesToRender = cache.cachedLines;
        } else {
            linesToRender = createHighlightLines(selection.blocks(), selection.origin());
            cache.update(selection.origin(), selection.shapeIndex(), selection.blockLimit(), selection.blocks(), linesToRender);
        }

        Camera renderInfo = mc.gameRenderer.getMainCamera();
        Vec3 projectedView = renderInfo.position();
        assert poseStack != null;
        poseStack.pushPose();
        poseStack.translate(
                selection.origin().getX() - projectedView.x,
                selection.origin().getY() - projectedView.y,
                selection.origin().getZ() - projectedView.z
        );

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float lineWidth = mc.getWindow().getAppropriateLineWidth();

        VertexConsumer translucentBuilder = buffers.getBuffer(LINES_TRANSLUCENT_NO_DEPTH_TEST);
        int translucentColor = LiteminerClient.CONFIG.highlightSeeThroughLineColor.get().argb(0x4B);
        renderHighlight(poseStack, translucentBuilder, linesToRender, translucentColor, lineWidth);

        VertexConsumer opaqueBuilder = buffers.getBuffer(LINES_NORMAL);
        int opaqueColor = LiteminerClient.CONFIG.highlightForegroundLineColor.get().argb(0xFF);
        renderHighlight(poseStack, opaqueBuilder, linesToRender, opaqueColor, lineWidth);

        buffers.endBatch(LINES_TRANSLUCENT_NO_DEPTH_TEST);
        buffers.endBatch(LINES_NORMAL);

        poseStack.popPose();
        return InteractionResult.PASS;
    }

    private static void renderHighlight(PoseStack poseStack, VertexConsumer builder, List<Line> lines, int color,
            float width) {
        for (Line line : lines) {
            addLine(poseStack, builder, line.x1, line.y1, line.z1, line.x2, line.y2, line.z2, color, width);
        }
    }

    private static List<Line> createHighlightLines(HashSet<BlockPos> blocks, BlockPos origin) {
        if (blocks.size() > GRID_EXTERIOR_THRESHOLD) {
            return createGridExteriorLines(blocks, origin);
        }
        return createMergedExteriorLines(blocks, origin);
    }

    private static List<Line> createMergedExteriorLines(HashSet<BlockPos> blocks, BlockPos origin) {
        List<AABB> boxes = new ArrayList<>(WorldFunctions.mergeBoundingBoxes(blocks, origin));
        HashSet<Line> lines = new HashSet<>();
        for (AABB box : boxes) {
            addExteriorBoxLines(lines, boxes, box);
        }
        return mergeCollinearLines(lines);
    }

    private static List<Line> createGridExteriorLines(HashSet<BlockPos> blocks, BlockPos origin) {
        HashSet<Line> lines = new HashSet<>();
        for (BlockPos pos : blocks) {
            addGridExteriorBoxLines(lines, blocks, pos, origin);
        }
        return mergeCollinearLines(lines);
    }

    private static void addExteriorBoxLines(Collection<Line> lines, List<AABB> boxes, AABB box) {
        addExteriorLine(lines, boxes, Axis.X, box.minX, box.maxX, box.minY, box.minZ);
        addExteriorLine(lines, boxes, Axis.X, box.minX, box.maxX, box.maxY, box.minZ);
        addExteriorLine(lines, boxes, Axis.X, box.minX, box.maxX, box.minY, box.maxZ);
        addExteriorLine(lines, boxes, Axis.X, box.minX, box.maxX, box.maxY, box.maxZ);

        addExteriorLine(lines, boxes, Axis.Y, box.minY, box.maxY, box.minX, box.minZ);
        addExteriorLine(lines, boxes, Axis.Y, box.minY, box.maxY, box.maxX, box.minZ);
        addExteriorLine(lines, boxes, Axis.Y, box.minY, box.maxY, box.minX, box.maxZ);
        addExteriorLine(lines, boxes, Axis.Y, box.minY, box.maxY, box.maxX, box.maxZ);

        addExteriorLine(lines, boxes, Axis.Z, box.minZ, box.maxZ, box.minX, box.minY);
        addExteriorLine(lines, boxes, Axis.Z, box.minZ, box.maxZ, box.maxX, box.minY);
        addExteriorLine(lines, boxes, Axis.Z, box.minZ, box.maxZ, box.minX, box.maxY);
        addExteriorLine(lines, boxes, Axis.Z, box.minZ, box.maxZ, box.maxX, box.maxY);
    }

    private static void addGridExteriorBoxLines(Collection<Line> lines, HashSet<BlockPos> blocks, BlockPos pos,
            BlockPos origin) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        addGridExteriorLine(lines, blocks, Axis.X, x, x + 1, y, z, origin);
        addGridExteriorLine(lines, blocks, Axis.X, x, x + 1, y + 1, z, origin);
        addGridExteriorLine(lines, blocks, Axis.X, x, x + 1, y, z + 1, origin);
        addGridExteriorLine(lines, blocks, Axis.X, x, x + 1, y + 1, z + 1, origin);

        addGridExteriorLine(lines, blocks, Axis.Y, y, y + 1, x, z, origin);
        addGridExteriorLine(lines, blocks, Axis.Y, y, y + 1, x + 1, z, origin);
        addGridExteriorLine(lines, blocks, Axis.Y, y, y + 1, x, z + 1, origin);
        addGridExteriorLine(lines, blocks, Axis.Y, y, y + 1, x + 1, z + 1, origin);

        addGridExteriorLine(lines, blocks, Axis.Z, z, z + 1, x, y, origin);
        addGridExteriorLine(lines, blocks, Axis.Z, z, z + 1, x + 1, y, origin);
        addGridExteriorLine(lines, blocks, Axis.Z, z, z + 1, x, y + 1, origin);
        addGridExteriorLine(lines, blocks, Axis.Z, z, z + 1, x + 1, y + 1, origin);
    }

    private static void addGridExteriorLine(Collection<Line> lines, HashSet<BlockPos> blocks, Axis axis,
            int start, int end, int fixedA, int fixedB, BlockPos origin) {
        double middle = start + 0.5d;
        if (!isGridExteriorEdge(blocks, axis, middle, fixedA, fixedB)) {
            return;
        }

        double relativeStart = start - axis.origin(origin);
        double relativeEnd = end - axis.origin(origin);
        double relativeFixedA = fixedA - axis.fixedAOrigin(origin);
        double relativeFixedB = fixedB - axis.fixedBOrigin(origin);
        lines.add(axis.line(relativeStart, relativeEnd, relativeFixedA, relativeFixedB));
    }

    private static void addExteriorLine(Collection<Line> lines, List<AABB> boxes, Axis axis, double start, double end,
            double fixedA, double fixedB) {
        ArrayList<Double> splitPoints = new ArrayList<>();
        splitPoints.add(start);
        splitPoints.add(end);
        for (AABB box : boxes) {
            addSplitPoint(splitPoints, axis.min(box), start, end);
            addSplitPoint(splitPoints, axis.max(box), start, end);
        }
        splitPoints.sort(Double::compareTo);

        double segmentStart = splitPoints.get(0);
        for (int i = 1; i < splitPoints.size(); i++) {
            double segmentEnd = splitPoints.get(i);
            if (segmentEnd - segmentStart <= SAMPLE_EPSILON) {
                continue;
            }

            double middle = (segmentStart + segmentEnd) * 0.5d;
            if (isExteriorUnionEdge(boxes, axis, middle, fixedA, fixedB)) {
                lines.add(axis.line(segmentStart, segmentEnd, fixedA, fixedB));
            }
            segmentStart = segmentEnd;
        }
    }

    private static void addSplitPoint(Collection<Double> splitPoints, double value, double start, double end) {
        if (value > start + SAMPLE_EPSILON && value < end - SAMPLE_EPSILON) {
            splitPoints.add(value);
        }
    }

    private static boolean isExteriorUnionEdge(List<AABB> boxes, Axis axis, double middle, double fixedA,
            double fixedB) {
        boolean a = axis.contains(boxes, middle, fixedA - SAMPLE_EPSILON, fixedB - SAMPLE_EPSILON);
        boolean b = axis.contains(boxes, middle, fixedA - SAMPLE_EPSILON, fixedB + SAMPLE_EPSILON);
        boolean c = axis.contains(boxes, middle, fixedA + SAMPLE_EPSILON, fixedB - SAMPLE_EPSILON);
        boolean d = axis.contains(boxes, middle, fixedA + SAMPLE_EPSILON, fixedB + SAMPLE_EPSILON);
        return isExteriorQuadrantPattern(a, b, c, d);
    }

    private static boolean isGridExteriorEdge(HashSet<BlockPos> blocks, Axis axis, double middle, int fixedA,
            int fixedB) {
        boolean a = axis.contains(blocks, middle, fixedA - SAMPLE_EPSILON, fixedB - SAMPLE_EPSILON);
        boolean b = axis.contains(blocks, middle, fixedA - SAMPLE_EPSILON, fixedB + SAMPLE_EPSILON);
        boolean c = axis.contains(blocks, middle, fixedA + SAMPLE_EPSILON, fixedB - SAMPLE_EPSILON);
        boolean d = axis.contains(blocks, middle, fixedA + SAMPLE_EPSILON, fixedB + SAMPLE_EPSILON);
        return isExteriorQuadrantPattern(a, b, c, d);
    }

    private static boolean isExteriorQuadrantPattern(boolean a, boolean b, boolean c, boolean d) {
        int solidQuadrants = (a ? 1 : 0) + (b ? 1 : 0) + (c ? 1 : 0) + (d ? 1 : 0);
        boolean diagonal = (a && d && !b && !c) || (b && c && !a && !d);
        return solidQuadrants == 1 || solidQuadrants == 3 || diagonal;
    }

    private static boolean contains(List<AABB> boxes, double x, double y, double z) {
        for (AABB box : boxes) {
            if (x > box.minX && x < box.maxX &&
                    y > box.minY && y < box.maxY &&
                    z > box.minZ && z < box.maxZ) {
                return true;
            }
        }
        return false;
    }

    private static boolean contains(HashSet<BlockPos> blocks, double x, double y, double z) {
        return blocks.contains(new BlockPos(
                (int) Math.floor(x),
                (int) Math.floor(y),
                (int) Math.floor(z)
        ));
    }

    private static List<Line> mergeCollinearLines(Collection<Line> lines) {
        Map<LineKey, List<Line>> groups = new HashMap<>();
        for (Line line : lines) {
            groups.computeIfAbsent(LineKey.of(line), ignored -> new ArrayList<>()).add(line);
        }

        ArrayList<Line> merged = new ArrayList<>();
        for (Map.Entry<LineKey, List<Line>> entry : groups.entrySet()) {
            List<Line> group = entry.getValue();
            Axis axis = entry.getKey().axis();
            group.sort(Comparator.comparingDouble(axis::start));

            double start = axis.start(group.get(0));
            double end = axis.end(group.get(0));
            double fixedA = entry.getKey().fixedA();
            double fixedB = entry.getKey().fixedB();
            for (int i = 1; i < group.size(); i++) {
                Line line = group.get(i);
                double nextStart = axis.start(line);
                double nextEnd = axis.end(line);
                if (nextStart <= end + SAMPLE_EPSILON) {
                    end = Math.max(end, nextEnd);
                } else {
                    merged.add(axis.line(start, end, fixedA, fixedB));
                    start = nextStart;
                    end = nextEnd;
                }
            }
            merged.add(axis.line(start, end, fixedA, fixedB));
        }
        return merged;
    }

    private static void addLine(PoseStack poseStack, VertexConsumer builder, double x1, double y1, double z1,
            double x2, double y2, double z2, int color, float width) {
        PoseStack.Pose pose = poseStack.last();
        Vector3f normal = new Vector3f((float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1)).normalize();
        builder.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(color).setNormal(pose, normal).setLineWidth(width);
        builder.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(color).setNormal(pose, normal).setLineWidth(width);
    }

    private static long cacheTimeoutMillis(int blockLimit) {
        return blockLimit >= 512 ? 500L :
                blockLimit >= 256 ? 300L :
                blockLimit >= 128 ? 200L :
                100L;
    }

    private record Line(double x1, double y1, double z1, double x2, double y2, double z2) {}

    private record LineKey(Axis axis, double fixedA, double fixedB) {
        private static LineKey of(Line line) {
            if (line.y1 == line.y2 && line.z1 == line.z2) {
                return new LineKey(Axis.X, line.y1, line.z1);
            }
            if (line.x1 == line.x2 && line.z1 == line.z2) {
                return new LineKey(Axis.Y, line.x1, line.z1);
            }
            return new LineKey(Axis.Z, line.x1, line.y1);
        }
    }

    private enum Axis {
        X {
            @Override
            double min(AABB box) {
                return box.minX;
            }

            @Override
            double max(AABB box) {
                return box.maxX;
            }

            @Override
            double start(Line line) {
                return line.x1;
            }

            @Override
            double end(Line line) {
                return line.x2;
            }

            @Override
            boolean contains(List<AABB> boxes, double middle, double fixedA, double fixedB) {
                return BlockHighlightRenderer.contains(boxes, middle, fixedA, fixedB);
            }

            @Override
            boolean contains(HashSet<BlockPos> blocks, double middle, double fixedA, double fixedB) {
                return BlockHighlightRenderer.contains(blocks, middle, fixedA, fixedB);
            }

            @Override
            Line line(double start, double end, double fixedA, double fixedB) {
                return new Line(start, fixedA, fixedB, end, fixedA, fixedB);
            }

            @Override
            int origin(BlockPos origin) {
                return origin.getX();
            }

            @Override
            int fixedAOrigin(BlockPos origin) {
                return origin.getY();
            }

            @Override
            int fixedBOrigin(BlockPos origin) {
                return origin.getZ();
            }
        },
        Y {
            @Override
            double min(AABB box) {
                return box.minY;
            }

            @Override
            double max(AABB box) {
                return box.maxY;
            }

            @Override
            double start(Line line) {
                return line.y1;
            }

            @Override
            double end(Line line) {
                return line.y2;
            }

            @Override
            boolean contains(List<AABB> boxes, double middle, double fixedA, double fixedB) {
                return BlockHighlightRenderer.contains(boxes, fixedA, middle, fixedB);
            }

            @Override
            boolean contains(HashSet<BlockPos> blocks, double middle, double fixedA, double fixedB) {
                return BlockHighlightRenderer.contains(blocks, fixedA, middle, fixedB);
            }

            @Override
            Line line(double start, double end, double fixedA, double fixedB) {
                return new Line(fixedA, start, fixedB, fixedA, end, fixedB);
            }

            @Override
            int origin(BlockPos origin) {
                return origin.getY();
            }

            @Override
            int fixedAOrigin(BlockPos origin) {
                return origin.getX();
            }

            @Override
            int fixedBOrigin(BlockPos origin) {
                return origin.getZ();
            }
        },
        Z {
            @Override
            double min(AABB box) {
                return box.minZ;
            }

            @Override
            double max(AABB box) {
                return box.maxZ;
            }

            @Override
            double start(Line line) {
                return line.z1;
            }

            @Override
            double end(Line line) {
                return line.z2;
            }

            @Override
            boolean contains(List<AABB> boxes, double middle, double fixedA, double fixedB) {
                return BlockHighlightRenderer.contains(boxes, fixedA, fixedB, middle);
            }

            @Override
            boolean contains(HashSet<BlockPos> blocks, double middle, double fixedA, double fixedB) {
                return BlockHighlightRenderer.contains(blocks, fixedA, fixedB, middle);
            }

            @Override
            Line line(double start, double end, double fixedA, double fixedB) {
                return new Line(fixedA, fixedB, start, fixedA, fixedB, end);
            }

            @Override
            int origin(BlockPos origin) {
                return origin.getZ();
            }

            @Override
            int fixedAOrigin(BlockPos origin) {
                return origin.getX();
            }

            @Override
            int fixedBOrigin(BlockPos origin) {
                return origin.getY();
            }
        };

        abstract double min(AABB box);

        abstract double max(AABB box);

        abstract double start(Line line);

        abstract double end(Line line);

        abstract boolean contains(List<AABB> boxes, double middle, double fixedA, double fixedB);

        abstract boolean contains(HashSet<BlockPos> blocks, double middle, double fixedA, double fixedB);

        abstract Line line(double start, double end, double fixedA, double fixedB);

        abstract int origin(BlockPos origin);

        abstract int fixedAOrigin(BlockPos origin);

        abstract int fixedBOrigin(BlockPos origin);
    }
}
