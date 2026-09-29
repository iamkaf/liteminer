pluginManagement {
    repositories {
        // Resolve workspace plugins only from Maven local and Kaf Maven.
        exclusiveContent {
            forRepositories(mavenLocal(), maven("https://maven.kaf.sh") { name = "Kaf Maven" })
            filter { includeGroupByRegex("com\\.iamkaf(\\..*)?") }
        }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("com.iamkaf.multiloader.settings") version providers.gradleProperty("project.plugins").get()
}
