plugins {
    id("dev.kikugie.stonecutter")
    id("fabric-loom") apply false
    id("net.fabricmc.fabric-loom") apply false
    id("com.iamkaf.multiloader.root")
}

stonecutter active "26.1.2".let { multiloaderStonecutter.active(it) }
