import com.iamkaf.multiloader.support.MultiloaderProjectContext

plugins {
    id("com.iamkaf.multiloader.common")
}

val multiloader = MultiloaderProjectContext.of(project)

dependencies {
    // Open Parties and Claims API, for keeping vein mining out of claims.
    compileOnly(multiloader.catalogFor().findLibrary("opac-common").get()) {
        isTransitive = false
    }
    multiloader.optionalProperty("dependencies.iris")?.let { irisVersion ->
        compileOnly("maven.modrinth:iris:$irisVersion") {
            isTransitive = false
        }
    }
}

sourceSets.named("test") {
    java.srcDir(rootProject.file("test/unit"))
    compileClasspath += sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().compileClasspath
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
