pluginManagement {
    val quarkusPluginVersion: String by settings
    plugins {
        id("io.quarkus.application") version quarkusPluginVersion
    }
}

rootProject.name = "standalone-application-plugin-advanced"
