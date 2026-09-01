import com.iamkaf.multiloader.support.MultiloaderProjectContext

plugins {
    id("com.iamkaf.multiloader.fabric")
}

val multiloader = MultiloaderProjectContext.of(project)
val minecraftVersion = multiloader.minecraftVersion()
val catalog = multiloader.catalogFor()

dependencies {
    multiloader.optionalProperty("dependencies.iris")?.let { irisVersion ->
        val irisConfiguration = if (multiloader.useUnobfuscatedMinecraft()) "compileOnly" else "modCompileOnly"
        add(irisConfiguration, "maven.modrinth:iris:$irisVersion") {
            isTransitive = false
        }
    }

    if (minecraftVersion !in setOf("26.2", "26.3")) {
        val configuration = if (multiloader.useUnobfuscatedMinecraft()) "implementation" else "modImplementation"
        add(configuration, multiloader.library(catalog, "forgeconfigapiport-fabric"))
    }
}
