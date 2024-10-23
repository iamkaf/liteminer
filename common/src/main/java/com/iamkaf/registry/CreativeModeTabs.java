package com.iamkaf.registry;

import com.iamkaf.template.Template;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class CreativeModeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Template.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> TEMPLATE = TABS.register(Template.MOD_ID,
            () -> CreativeTabRegistry.create(Component.translatable("creativetab." + Template.MOD_ID + "." + Template.MOD_ID),
                    () -> new ItemStack(Items.EXAMPLE_ITEM.get())
            )
    );

    public static void init() {
        TABS.register();
    }
}