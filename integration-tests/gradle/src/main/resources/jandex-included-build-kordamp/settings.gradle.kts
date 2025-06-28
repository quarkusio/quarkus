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
    }
}
rootProject.name = "jandex-included-build-kordamp-root"

includeBuild("acme-subproject-nested")
