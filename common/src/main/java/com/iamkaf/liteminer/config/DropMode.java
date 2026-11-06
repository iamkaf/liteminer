package com.iamkaf.liteminer.config;

import java.util.Locale;

/** Where the items and experience of every block after the first one land. */
public enum DropMode {
    TOGETHER,
    EACH_BLOCK;

    public String translationKey() {
        return "liteminer.config.drop_mode." + name().toLowerCase(Locale.ROOT);
    }
}
