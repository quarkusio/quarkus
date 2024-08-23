plugins {
    id("io.quarkus.devtools.java-library")
}

dependencies {
    compileOnly(libs.kotlin.gradle.plugin.api)
    gradleApi()
}

group = "io.quarkus"

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications.create<MavenPublication>("maven") {
        artifactId = "quarkus-gradle-model"
        from(components["java"])
    }
}
