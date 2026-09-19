pluginManagement {
    repositories {
        mavenLocal()
        maven("https://maven.kaf.sh") { name = "Kaf Maven" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("com.iamkaf.multiloader.settings") version providers.gradleProperty("project.plugins").get()
}

// The confirmation protocol and Doctor contributors require Amber 11.4.0.
dependencyResolutionManagement {
    for (minecraft in listOf("1.21.11", "26.1", "26.1.1", "26.1.2", "26.2", "26.3")) {
        versionCatalogs.findByName("libsMc${minecraft.replace(".", "")}")
            ?.version("amber", "11.4.0+$minecraft")
    }
}
