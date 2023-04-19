plugins {
    id("io.quarkus.devtools.java-library")
}

dependencies {
    implementation(libs.kotlin.gradle.plugin.api)
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
