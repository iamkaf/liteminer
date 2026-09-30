package com.iamkaf.liteminer.config;

//? if <26.2 {
/*import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;
*///?}

import java.util.Locale;

/** Where the items and experience of every block after the first one land. */
public enum DropMode
        //? if <26.2
        /*implements TranslatableEnum*/
{
    TOGETHER,
    EACH_BLOCK;

    public String translationKey() {
        return "liteminer.config.drop_mode." + name().toLowerCase(Locale.ROOT);
    }

    //? if <26.2 {
    /*@Override
    public Component getTranslatedName() {
        return Component.translatable(translationKey());
    }
    *///?}
}
