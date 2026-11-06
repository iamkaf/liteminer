plugins {
    id("dev.kikugie.stonecutter")
    id("fabric-loom") apply false
    id("net.fabricmc.fabric-loom") apply false
    id("com.iamkaf.multiloader.root")
}

stonecutter active "26.3".let { multiloaderStonecutter.active(it) }

stonecutter parameters {
    replacements.string(eval(current.version, ">=1.21.11")) {
        replace("ResourceLocation", "Identifier")
    }
}
