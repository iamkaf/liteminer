import com.iamkaf.multiloader.support.MultiloaderProjectContext

plugins {
    id("com.iamkaf.multiloader.neoforge")
}

val multiloader = MultiloaderProjectContext.of(project)

dependencies {
    multiloader.optionalProperty("dependencies.iris")?.let { irisVersion ->
        compileOnly("maven.modrinth:iris:$irisVersion") {
            isTransitive = false
        }
    }
}
