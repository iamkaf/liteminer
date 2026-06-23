plugins {
    id("dev.kikugie.stonecutter")
    id("com.iamkaf.multiloader.root")
}

stonecutter active "26.1.2".let { multiloaderStonecutter.active(it) }
