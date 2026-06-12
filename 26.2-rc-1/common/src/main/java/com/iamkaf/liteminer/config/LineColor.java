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

    public int transitioningArgb(int alpha8, long timeMillis, long periodMillis) {
        return ((alpha8 & 0xFF) << 24) | transitionRgb(timeMillis, periodMillis);
    }

    int transitionRgb(long timeMillis, long periodMillis) {
        if (periodMillis <= 0) {
            return oppositeRgb24();
        }

        double phase = (timeMillis % periodMillis) / (double) periodMillis;
        double factor = (1.0d - Math.cos(phase * Math.PI * 2.0d)) * 0.5d;
        return lerpRgb(rgb24, oppositeRgb24(), factor);
    }

    int oppositeRgb24() {
        return (~rgb24) & 0xFFFFFF;
    }

    private static int lerpRgb(int from, int to, double factor) {
        int red = lerpChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, factor);
        int green = lerpChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, factor);
        int blue = lerpChannel(from & 0xFF, to & 0xFF, factor);
        return (red << 16) | (green << 8) | blue;
    }

    private static int lerpChannel(int from, int to, double factor) {
        return (int) Math.round(from + (to - from) * factor);
    }
}
