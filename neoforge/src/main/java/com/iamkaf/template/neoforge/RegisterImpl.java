package com.iamkaf.template.neoforge;

import com.iamkaf.template.Template;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class RegisterImpl {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.createBlocks(Template.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.createItems(Template.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Template.MOD_ID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Template.MOD_ID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIAL =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, Template.MOD_ID);

    public static <T extends Block> Supplier<T> block(String id, Supplier<T> supplier) {
        var block = BLOCKS.register(id, supplier);
        item(id, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    public static <T extends Item> Supplier<T> item(String id, Supplier<T> supplier) {
        return ITEMS.register(id, supplier);
    }

    public static <T extends Item> void fuelItem(Supplier<T> supplier, int burnTime) {
        // NO-OP, done in datapack
    }

//    public static Supplier<CreativeModeTab> creativeModeTab() {
//        var tab = TABS.register(
//                "amberdreams_tab",
//                () -> CreativeModeTab.builder()
//                        .icon(() -> new ItemStack(Template.Blocks.BISMUTH_BLOCK.get()))
//                        .title(Component.translatable("creativetab.amberdreams.amberdreams_tab"))
//                        .displayItems((itemDisplayParameters, output) -> {
//                            for (var item : Template.CreativeModeTabs.getCreativeModeTabItems()) {
//                                output.accept(item);
//                            }
//                        })
//                        .build()
//        );
//
//        return () -> tab.get();
//    }

    public static <T> Supplier<DataComponentType<T>> dataComponentType(String name,
            UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return DATA_COMPONENT_TYPES.register(name,
                () -> builderOperator.apply(DataComponentType.builder()).build()
        );
    }

    public static Holder<ArmorMaterial> armorMaterial(String name, ArmorMaterial material) {
        var obj = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL,
                Template.resource(name),
                material
        );
        return obj;
    }
}
