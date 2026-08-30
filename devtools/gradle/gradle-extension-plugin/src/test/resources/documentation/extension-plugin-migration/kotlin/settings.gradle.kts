// Source: {{source}}
// tag::three-x-plugin-management[]
pluginManagement {
    val quarkusPluginVersion: String by settings
    plugins {
        id("io.quarkus.extension") version quarkusPluginVersion
    }
}
// end::three-x-plugin-management[]

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "acme-extension"
include("runtime", "deployment")
