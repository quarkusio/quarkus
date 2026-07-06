plugins {
    id("io.quarkus.devtools.gradle-plugin")
}

dependencies {
    testImplementation(testFixtures(project(":gradle-model")))
}

group = "io.quarkus.extension.deployment"

gradlePlugin {
    plugins.create("quarkusExtensionDeploymentPlugin") {
        id = "io.quarkus.extension.deployment"
        implementationClass = "io.quarkus.extension.deployment.gradle.QuarkusExtensionDeploymentPlugin"
        displayName = "Quarkus Extension Deployment Plugin"
        description = "Builds a Quarkus extension deployment module"
        tags.addAll("quarkus", "quarkusio", "graalvm")
    }
}

tasks.withType<Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
