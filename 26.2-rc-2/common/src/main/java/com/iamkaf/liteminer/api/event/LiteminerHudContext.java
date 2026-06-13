package com.iamkaf.liteminer.api.event;

import com.iamkaf.liteminer.api.shape.LiteminerShape;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable context for Liteminer's default HUD rendering.
 */
public final class LiteminerHudContext {
    private final int selectedBlockCount;
    private final LiteminerShape selectedShape;
    private final List<Component> lines;
    private boolean visible = true;
    private int textColor = 0xFFFFFFFF;
    private int lineHeight = 10;
    private int xOffset = 5;
    private int yOffset = -10;

    /**
     * Creates a HUD context.
     *
     * @param selectedBlockCount number of currently highlighted candidate blocks
     * @param selectedShape      currently selected Liteminer shape
     * @param lines              initial HUD lines; copied into a mutable list
     */
    public LiteminerHudContext(int selectedBlockCount, LiteminerShape selectedShape, List<Component> lines) {
        this.selectedBlockCount = selectedBlockCount;
        this.selectedShape = selectedShape;
        this.lines = new ArrayList<>(lines);
    }

    /**
     * Returns the number of currently highlighted candidate blocks.
     *
     * @return selected block count
     */
    public int selectedBlockCount() {
        return selectedBlockCount;
    }

    /**
     * Returns the currently selected shape.
     *
     * @return selected shape
     */
    public LiteminerShape selectedShape() {
        return selectedShape;
    }

    /**
     * Returns mutable HUD lines.
     *
     * <p>Callbacks can add, remove, replace, or clear entries in this list.</p>
     *
     * @return mutable HUD line list
     */
    public List<Component> lines() {
        return lines;
    }

    /**
     * Returns whether the default HUD should be rendered.
     *
     * @return {@code true} if the HUD remains visible
     */
    public boolean visible() {
        return visible;
    }

    /**
     * Sets whether the default HUD should be rendered.
     *
     * @param visible {@code false} to suppress the HUD for this frame
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Returns the ARGB text color used for all default HUD lines.
     *
     * @return ARGB text color
     */
    public int textColor() {
        return textColor;
    }

    /**
     * Sets the ARGB text color used for all default HUD lines.
     *
     * @param textColor ARGB text color
     */
    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    /**
     * Returns the vertical distance between HUD lines before UI scale is applied.
     *
     * @return line height in GUI pixels
     */
    public int lineHeight() {
        return lineHeight;
    }

    /**
     * Sets the vertical distance between HUD lines before UI scale is applied.
     *
     * @param lineHeight line height in GUI pixels
     */
    public void setLineHeight(int lineHeight) {
        this.lineHeight = lineHeight;
    }

    /**
     * Returns the horizontal offset from the screen center before UI scale is applied.
     *
     * @return x offset in GUI pixels
     */
    public int xOffset() {
        return xOffset;
    }

    /**
     * Sets the horizontal offset from the screen center before UI scale is applied.
     *
     * @param xOffset x offset in GUI pixels
     */
    public void setXOffset(int xOffset) {
        this.xOffset = xOffset;
    }

    /**
     * Returns the vertical offset from the screen center before UI scale is applied.
     *
     * @return y offset in GUI pixels
     */
    public int yOffset() {
        return yOffset;
    }

    /**
     * Sets the vertical offset from the screen center before UI scale is applied.
     *
     * @param yOffset y offset in GUI pixels
     */
    public void setYOffset(int yOffset) {
        this.yOffset = yOffset;
    }
}
