pluginManagement {
    val quarkusPluginVersion = providers.gradleProperty("quarkusPluginVersion").get()
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("io.quarkus.extension") version quarkusPluginVersion
        id("io.quarkus.extension.deployment") version quarkusPluginVersion
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "quarkus-gradle-demo-extension"

include(":runtime", ":deployment")

project(":runtime").name = "demo-greeting-extension"
