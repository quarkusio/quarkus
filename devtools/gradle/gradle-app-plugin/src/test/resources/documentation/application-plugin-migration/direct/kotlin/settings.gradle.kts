// Source: {{source}}
// tag::direct-legacy-plugin-management[]
pluginManagement {
    val quarkusPluginVersion: String by settings
    plugins {
        id("io.quarkus") version quarkusPluginVersion
    }
}
// end::direct-legacy-plugin-management[]

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "application-plugin-direct-migration"
