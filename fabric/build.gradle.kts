import com.iamkaf.multiloader.support.MultiloaderProjectContext

plugins {
    id("com.iamkaf.multiloader.fabric")
}

val multiloader = MultiloaderProjectContext.of(project)

dependencies {
    multiloader.optionalProperty("dependencies.iris")?.let { irisVersion ->
        val irisConfiguration = if (multiloader.useUnobfuscatedMinecraft()) "compileOnly" else "modCompileOnly"
        add(irisConfiguration, "maven.modrinth:iris:$irisVersion") {
            isTransitive = false
        }
    }
}
