import com.iamkaf.multiloader.support.MultiloaderProjectContext

plugins {
    id("com.iamkaf.multiloader.neoforge")
}

val multiloader = MultiloaderProjectContext.of(project)

dependencies {
    // Open Parties and Claims API, for keeping vein mining out of claims.
    compileOnly(multiloader.catalogFor().findLibrary("opac-common").get()) {
        isTransitive = false
    }
    // Open Parties and Claims at dev runtime wherever the catalog lists it, for the claim test. Non-transitive,
    // because OPAC's metadata can name a Forge Config API Port for another version; the catalog pins the right one.
    multiloader.catalogFor().findLibrary("opac-neoforge").ifPresent { addProvider<MinimalExternalModuleDependency, ExternalModuleDependency>("runtimeOnly", it) { isTransitive = false } }
    multiloader.optionalProperty("dependencies.iris")?.let { irisVersion ->
        compileOnly("maven.modrinth:iris:$irisVersion") {
            isTransitive = false
        }
    }
}
