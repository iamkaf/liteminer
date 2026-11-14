import com.iamkaf.multiloader.support.MultiloaderProjectContext

plugins {
    id("com.iamkaf.multiloader.fabric")
}

val multiloader = MultiloaderProjectContext.of(project)

// Forge Config API Port for OPAC at dev runtime on the obfuscated lines. Loom strips the night-config jars it
// bundles, and C2ME's older bundled copy then wins, so the jar goes into the mods folder whole, like C2ME's.
val opacFcapRuntime = configurations.create("opacFcapRuntime") { isTransitive = false }
val stageOpacFcap = tasks.register<Copy>("stageOpacFcapClientRuntimeMod") {
    from(opacFcapRuntime)
    into(layout.projectDirectory.dir("runs/client/mods"))
}
tasks.matching { it.name == "runClient" }.configureEach { dependsOn(stageOpacFcap) }

dependencies {
    // Open Parties and Claims API, for keeping vein mining out of claims.
    compileOnly(multiloader.catalogFor().findLibrary("opac-common").get()) {
        isTransitive = false
    }
    // Open Parties and Claims at dev runtime wherever the catalog lists it, for the claim test. Non-transitive,
    // because OPAC's metadata can name a Forge Config API Port for another version; the catalog pins the right one.
    val opacRuntime = if (multiloader.useUnobfuscatedMinecraft()) "runtimeOnly" else "modRuntimeOnly"
    multiloader.catalogFor().findLibrary("opac-fabric").ifPresent { opac ->
        addProvider<MinimalExternalModuleDependency, ExternalModuleDependency>(opacRuntime, opac) { isTransitive = false }
        multiloader.catalogFor().findLibrary("opac-fcap-fabric").ifPresent { fcap ->
            if (multiloader.useUnobfuscatedMinecraft()) {
                addProvider<MinimalExternalModuleDependency, ExternalModuleDependency>("runtimeOnly", fcap) { isTransitive = false }
            } else {
                addProvider<MinimalExternalModuleDependency, ExternalModuleDependency>(opacFcapRuntime.name, fcap) { isTransitive = false }
            }
        }
    }
    multiloader.optionalProperty("dependencies.iris")?.let { irisVersion ->
        val irisConfiguration = if (multiloader.useUnobfuscatedMinecraft()) "compileOnly" else "modCompileOnly"
        add(irisConfiguration, "maven.modrinth:iris:$irisVersion") {
            isTransitive = false
        }
    }
}
