import com.iamkaf.multiloader.support.MultiloaderProjectContext

plugins {
    id("com.iamkaf.multiloader.common")
}

val multiloader = MultiloaderProjectContext.of(project)
val minecraftVersion = multiloader.minecraftVersion()
val catalog = multiloader.catalogFor()

dependencies {
    multiloader.optionalProperty("dependencies.iris")?.let { irisVersion ->
        compileOnly("maven.modrinth:iris:$irisVersion") {
            isTransitive = false
        }
    }

    // TODO: remove this once we drop forgeconfigapiport in favor of konfig
    if (minecraftVersion !in setOf("26.2", "26.3")) {
        add("implementation", multiloader.library(catalog, "forgeconfigapiport-common-neoforgeapi"))
    }
}
