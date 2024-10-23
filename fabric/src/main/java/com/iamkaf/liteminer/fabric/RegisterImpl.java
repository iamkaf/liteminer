package com.iamkaf.liteminer.fabric;

import com.iamkaf.liteminer.Liteminer;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class RegisterImpl {

    public static <T extends Block> Supplier<T> block(String id, Supplier<T> supplier) {
        var obj = Registry.register(BuiltInRegistries.BLOCK, Liteminer.resource(id), supplier.get());
        item(id, () -> new BlockItem(obj, new Item.Properties()));
        return () -> obj;
    }

    public static <T extends Item> Supplier<T> item(String id, Supplier<T> supplier) {
        var obj = Registry.register(BuiltInRegistries.ITEM, Liteminer.resource(id), supplier.get());
        return () -> obj;
    }

    public static <T extends Item> void fuelItem(Supplier<T> supplier, int burnTime) {
        FuelRegistry.INSTANCE.add(supplier.get(), burnTime);
    }

//    public static Supplier<CreativeModeTab> creativeModeTab() {
//        var obj = Registry.register(
//                BuiltInRegistries.CREATIVE_MODE_TAB,
//                Template.resource("amberdreams_tab"),
//                FabricItemGroup.builder()
//                        .icon(() -> new ItemStack(Template.Blocks.BISMUTH_BLOCK.get()))
//                        .title(Component.translatable("creativetab.amberdreams.amberdreams_tab"))
//                        .displayItems((itemDisplayParameters, output) -> {
//                            for (var item : Template.CreativeModeTabs.getCreativeModeTabItems()) {
//                                output.accept(item);
//                            }
//                        })
//                        .build()
//        );
//        return () -> obj;
//    }

    public static <T> Supplier<DataComponentType<T>> dataComponentType(String name,
            UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        var obj = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                Liteminer.resource(name),
                builderOperator.apply(DataComponentType.builder()).build()
        );
        return () -> obj;
    }

    public static Holder<ArmorMaterial> armorMaterial(String name, ArmorMaterial material) {
        var obj = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL,
                Liteminer.resource(name),
                material
        );
        return obj;
    }
}
