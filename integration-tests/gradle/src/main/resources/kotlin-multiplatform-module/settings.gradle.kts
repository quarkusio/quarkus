pluginManagement {
    val quarkusPluginVersion: String by settings
    val quarkusPluginId: String by settings
    val kotlinVersion: String by settings
    repositories {
        mavenLocal {
            content {
                includeGroupByRegex("io.quarkus.*")
                includeGroup("org.hibernate.orm")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id(quarkusPluginId) version quarkusPluginVersion
        kotlin("jvm") version kotlinVersion
        kotlin("multiplatform") version kotlinVersion apply false
        kotlin("plugin.allopen") version kotlinVersion
    }
}
rootProject.name = "kotlin-multiplatform-module"

include("acme-kmp-lib")