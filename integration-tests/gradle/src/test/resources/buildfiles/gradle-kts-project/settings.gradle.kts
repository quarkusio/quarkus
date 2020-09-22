pluginManagement {
    val quarkusPluginVersion: String by settings
    repositories {
        if (System.getProperties().containsKey("maven.repo.local")) {
            maven {
                url = uri(System.getProperties().get("maven.repo.local") as String)
            }
        } else {
            mavenLocal()
        }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("io.quarkus") version quarkusPluginVersion
    }
}
rootProject.name="gradle-kts-project"