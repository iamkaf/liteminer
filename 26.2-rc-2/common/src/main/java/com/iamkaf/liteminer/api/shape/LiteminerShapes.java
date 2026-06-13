package com.iamkaf.liteminer.api.shape;

import com.iamkaf.liteminer.Constants;
import com.iamkaf.liteminer.shapes.ShapelessWalker;
import com.iamkaf.liteminer.shapes.StaircaseDownWalker;
import com.iamkaf.liteminer.shapes.StaircaseUpWalker;
import com.iamkaf.liteminer.shapes.ThreeByThreeWalker;
import com.iamkaf.liteminer.shapes.TunnelWalker;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Registry for Liteminer mining shapes.
 *
 * <p>Addons can register shapes during mod initialization. Registered shapes are available
 * to Liteminer's shape cycling, HUD, highlighting, and server-side veinmine logic.</p>
 */
public final class LiteminerShapes {
    private static final List<LiteminerShape> SHAPES = new ArrayList<>();
    private static final List<LiteminerShape> SHAPE_VIEW = Collections.unmodifiableList(SHAPES);

    /**
     * Built-in shapeless vein mining shape id.
     */
    public static final Identifier SHAPELESS = Constants.resource("shapeless");

    /**
     * Built-in small tunnel shape id.
     */
    public static final Identifier SMALL_TUNNEL = Constants.resource("small_tunnel");

    /**
     * Built-in staircase-up shape id.
     */
    public static final Identifier STAIRCASE_UP = Constants.resource("staircase_up");

    /**
     * Built-in staircase-down shape id.
     */
    public static final Identifier STAIRCASE_DOWN = Constants.resource("staircase_down");

    /**
     * Built-in 3x3 shape id.
     */
    public static final Identifier THREE_BY_THREE = Constants.resource("three_by_three");

    static {
        register(SHAPELESS, Component.translatable("shape.liteminer.shapeless"), new ShapelessWalker()::walk);
        register(SMALL_TUNNEL, Component.translatable("shape.liteminer.small_tunnel"), new TunnelWalker()::walk);
        register(STAIRCASE_UP, Component.translatable("shape.liteminer.staircase_up"), new StaircaseUpWalker()::walk);
        register(STAIRCASE_DOWN, Component.translatable("shape.liteminer.staircase_down"), new StaircaseDownWalker()::walk);
        register(THREE_BY_THREE, Component.translatable("shape.liteminer.three_by_three"), new ThreeByThreeWalker()::walk);
    }

    private LiteminerShapes() {
    }

    /**
     * Registers a new mining shape.
     *
     * @param id          stable namespaced id for the shape
     * @param displayName text shown in the Liteminer HUD and shape change messages
     * @param walker      candidate block provider for the shape
     * @return the registered shape
     * @throws IllegalArgumentException if another shape is already registered with {@code id}
     */
    public static LiteminerShape register(Identifier id, Component displayName, ShapeWalker walker) {
        if (get(id).isPresent()) {
            throw new IllegalArgumentException("Duplicate Liteminer shape id: " + id);
        }

        LiteminerShape shape = new LiteminerShape(id, displayName, walker);
        SHAPES.add(shape);
        return shape;
    }

    /**
     * Returns all registered shapes in cycling order.
     *
     * @return an unmodifiable live view of registered shapes
     */
    public static List<LiteminerShape> all() {
        return SHAPE_VIEW;
    }

    /**
     * Looks up a shape by id.
     *
     * @param id the shape id to find
     * @return the matching shape, or {@link Optional#empty()} if none is registered
     */
    public static Optional<LiteminerShape> get(Identifier id) {
        for (LiteminerShape shape : SHAPES) {
            if (shape.id().equals(id)) {
                return Optional.of(shape);
            }
        }
        return Optional.empty();
    }

    /**
     * Looks up a shape by cycling index.
     *
     * @param index shape index; values outside the list wrap with {@link Math#floorMod(int, int)}
     * @return the matching shape, or {@link Optional#empty()} if no shapes are registered
     */
    public static Optional<LiteminerShape> byIndex(int index) {
        if (SHAPES.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(SHAPES.get(Math.floorMod(index, SHAPES.size())));
    }

    /**
     * Returns the cycling index for a registered shape id.
     *
     * @param id the shape id to find
     * @return the shape index, or {@code -1} if none is registered
     */
    public static int indexOf(Identifier id) {
        for (int i = 0; i < SHAPES.size(); i++) {
            if (SHAPES.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the number of registered shapes.
     *
     * @return registered shape count
     */
    public static int size() {
        return SHAPES.size();
    }
}
