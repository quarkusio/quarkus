plugins {
    id("io.quarkus.devtools.gradle-plugin")
}

dependencies {
    implementation(libs.smallrye.config.yaml)
    implementation("io.quarkus:quarkus-analytics-common")
    compileOnly(libs.kotlin.gradle.plugin.api)
    testImplementation(libs.quarkus.project.core.extension.codestarts)
}

group = "io.quarkus"

gradlePlugin {
    plugins.create("quarkusPlugin") {
        id = "io.quarkus"
        implementationClass = "io.quarkus.gradle.QuarkusPlugin"
        displayName = "Quarkus Plugin"
        description =
            "Builds a Quarkus application, and provides helpers to launch dev-mode, the Quarkus CLI, building of native images"
        tags.addAll("quarkus", "quarkusio", "graalvm")
    }
}

tasks.test {
  systemProperty("kotlin_version", libs.versions.kotlin.get())
}
