// Source: {{source}}
// tag::legacy-first-plugin-management[]
pluginManagement {
    val quarkusPluginVersion: String by settings
    plugins {
        id("io.quarkus") version quarkusPluginVersion
        id("io.quarkus.application") version quarkusPluginVersion
    }
}
// end::legacy-first-plugin-management[]

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "application-plugin-coexistence-migration"
