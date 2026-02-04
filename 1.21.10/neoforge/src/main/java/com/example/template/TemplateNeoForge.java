package com.example.template;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class TemplateNeoForge {

    public TemplateNeoForge(IEventBus eventBus) {
        TemplateMod.init();
    }
}
