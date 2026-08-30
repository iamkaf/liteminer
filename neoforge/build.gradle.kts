import com.iamkaf.multiloader.support.MultiloaderProjectContext

plugins {
    id("com.iamkaf.multiloader.neoforge")
}

val multiloader = MultiloaderProjectContext.of(project)

dependencies {
    compileOnly("maven.modrinth:iris:${multiloader.requiredProperty("dependencies.iris")}") {
        isTransitive = false
    }
}
