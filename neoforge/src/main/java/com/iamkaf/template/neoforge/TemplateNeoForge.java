package com.iamkaf.template.neoforge;

import net.neoforged.fml.common.Mod;

import com.iamkaf.template.Template;

@Mod(Template.MOD_ID)
public final class TemplateNeoForge {
    public TemplateNeoForge() {
        // Run our common setup.
        Template.init();
    }
}
