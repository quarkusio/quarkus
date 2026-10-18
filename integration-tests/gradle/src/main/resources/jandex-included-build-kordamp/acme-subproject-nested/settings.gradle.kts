pluginManagement {
    val kordampJandexVersion: String by settings
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
        id("org.kordamp.gradle.jandex") version kordampJandexVersion
    }
}

rootProject.name = "jandex-included-build-kordamp-subproject-nested"

include("acme-nested")