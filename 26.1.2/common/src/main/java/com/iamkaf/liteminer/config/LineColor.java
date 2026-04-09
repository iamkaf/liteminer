package com.iamkaf.liteminer.config;

/**
 * Minecraft wool/dye palette (16 colors).
 *
 * Values are RGB hex (0xRRGGBB). Chosen to match Minecraft's DyeColor palette.
 */
public enum LineColor {
    WHITE(0xF9FFFE),
    ORANGE(0xF9801D),
    MAGENTA(0xC74EBD),
    LIGHT_BLUE(0x3AB3DA),
    YELLOW(0xFED83D),
    LIME(0x80C71F),
    PINK(0xF38BAA),
    GRAY(0x474F52),
    LIGHT_GRAY(0x9D9D97),
    CYAN(0x169C9C),
    PURPLE(0x8932B8),
    BLUE(0x3C44AA),
    BROWN(0x835432),
    GREEN(0x5E7C16),
    RED(0xB02E26),
    BLACK(0x1D1D21);

    private final int rgb24;

    LineColor(int rgb24) {
        this.rgb24 = rgb24;
    }

    public int argb(int alpha8) {
        return ((alpha8 & 0xFF) << 24) | (rgb24 & 0xFFFFFF);
    }
}
