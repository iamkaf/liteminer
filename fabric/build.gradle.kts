import com.iamkaf.multiloader.support.MultiloaderProjectContext

plugins {
    id("com.iamkaf.multiloader.fabric")
}

val multiloader = MultiloaderProjectContext.of(project)
val minecraftVersion = multiloader.minecraftVersion()
val catalog = multiloader.catalogFor()

dependencies {
    val irisConfiguration = if (multiloader.useUnobfuscatedMinecraft()) "compileOnly" else "modCompileOnly"
    add(irisConfiguration, "maven.modrinth:iris:${multiloader.requiredProperty("dependencies.iris")}") {
        isTransitive = false
    }

    if (minecraftVersion != "26.2") {
        val configuration = if (multiloader.useUnobfuscatedMinecraft()) "implementation" else "modImplementation"
        add(configuration, multiloader.library(catalog, "forgeconfigapiport-fabric"))
    }
}
