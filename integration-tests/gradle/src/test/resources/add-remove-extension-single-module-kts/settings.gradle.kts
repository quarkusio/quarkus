pluginManagement {
    val quarkusPluginVersion: String by settings
    repositories {
        if (System.getProperties().containsKey("maven.repo.local")) {
            maven(url = System.getProperties().get("maven.repo.local")!!)
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
rootProject.name="code-with-quarkus"