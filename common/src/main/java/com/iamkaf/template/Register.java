package com.iamkaf.template;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class Register {
    @ExpectPlatform
    public static <T extends Block> Supplier<T> block(String id, Supplier<T> supplier) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Item> Supplier<T> item(String id, Supplier<T> supplier) {
        throw new AssertionError();
    }

//    @ExpectPlatform
//    public static Supplier<CreativeModeTab> creativeModeTab() {
//        throw new AssertionError();
//    }

    @ExpectPlatform
    public static <T extends Item> void fuelItem(Supplier<T> supplier, int burnTime) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T> Supplier<DataComponentType<T>> dataComponentType(String name,
            UnaryOperator<DataComponentType.Builder<T>> builderUnaryOperator) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Holder<ArmorMaterial> armorMaterial(String name, ArmorMaterial material) {
        throw new AssertionError();
    }
}
