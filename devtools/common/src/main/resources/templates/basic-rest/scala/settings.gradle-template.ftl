pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == 'io.quarkus') {
                useModule("io.quarkus:quarkus-gradle-plugin:${quarkus_version}")
            }
        }
    }
}

rootProject.name='${project_artifactId}'
