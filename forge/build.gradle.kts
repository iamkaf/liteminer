import com.iamkaf.multiloader.support.MultiloaderProjectContext

plugins {
    id("com.iamkaf.multiloader.forge")
}

val multiloader = MultiloaderProjectContext.of(project)
val minecraftVersion = multiloader.minecraftVersion()
val catalog = multiloader.catalogFor()

dependencies {
    compileOnly("maven.modrinth:iris:${multiloader.requiredProperty("dependencies.iris")}") {
        isTransitive = false
    }

    if (minecraftVersion != "26.2") {
        add("implementation", multiloader.library(catalog, "forgeconfigapiport-forge"))
    }
}
