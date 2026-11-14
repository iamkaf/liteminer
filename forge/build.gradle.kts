import com.iamkaf.multiloader.support.MultiloaderProjectContext
import com.iamkaf.multiloader.support.VersionPolicy

plugins {
    id("com.iamkaf.multiloader.forge")
}

val multiloader = MultiloaderProjectContext.of(project)

dependencies {
    // Open Parties and Claims API, for keeping vein mining out of claims.
    compileOnly(multiloader.catalogFor().findLibrary("opac-common").get()) {
        isTransitive = false
    }
    // Open Parties and Claims at dev runtime wherever the catalog lists it, for the claim test. Non-transitive,
    // because OPAC's metadata can name a Forge Config API Port for another version; the catalog pins the right one.
    multiloader.catalogFor().findLibrary("opac-forge").ifPresent { opac ->
        val minecraft = multiloader.minecraftVersion()
        val opacRuntime = if (VersionPolicy.usesLegacyForgePlugin(minecraft)) "modRuntimeOnly" else "runtimeOnly"
        // OPAC's Forge jar uses SRG names here, and its dev jar uses the Mojang names this toolchain runs with.
        val opacJar = if (VersionPolicy.usesForgeGradleSrgRemap(minecraft)) variantOf(opac) { classifier("dev") } else opac
        addProvider<MinimalExternalModuleDependency, ExternalModuleDependency>(opacRuntime, opacJar) { isTransitive = false }
        multiloader.catalogFor().findLibrary("opac-fcap-forge").ifPresent { addProvider<MinimalExternalModuleDependency, ExternalModuleDependency>(opacRuntime, it) { isTransitive = false } }
    }
    multiloader.optionalProperty("dependencies.iris")?.let { irisVersion ->
        compileOnly("maven.modrinth:iris:$irisVersion") {
            isTransitive = false
        }
    }
}
