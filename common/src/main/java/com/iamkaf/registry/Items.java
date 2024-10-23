package com.iamkaf.registry;

import com.iamkaf.template.Template;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public class Items {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Template.MOD_ID, Registries.ITEM);

    public static RegistrySupplier<Item> EXAMPLE_ITEM = ITEMS.register("example_item",
            () -> new Item(new Item.Properties().arch$tab(CreativeModeTabs.TEMPLATE))
    );

    public static void init() {
        ITEMS.register();
    }
}