plugins {
    id("io.quarkus.devtools.gradle-plugin")
}

dependencies {
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.yaml)
}

group = "io.quarkus.extension"

gradlePlugin {
    plugins.create("quarkusBootstrapPlugin") {
        id = "io.quarkus.extension"
        implementationClass = "io.quarkus.extension.gradle.QuarkusExtensionPlugin"
        displayName = "Quarkus Extension Plugin"
        description = "Builds a Quarkus extension"
        tags.addAll("quarkus", "quarkusio", "graalvm")
    }
}

tasks.withType<Test>().configureEach {
    environment("GITHUB_REPOSITORY", "some/repo")
}

// to generate reproducible jars
tasks.withType<Jar>().configureEach {
    isPreserveFileTimestamps = false 
    isReproducibleFileOrder = true   
}
