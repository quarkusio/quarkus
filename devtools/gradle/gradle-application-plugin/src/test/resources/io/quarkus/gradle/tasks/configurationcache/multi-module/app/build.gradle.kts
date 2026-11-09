plugins {
    java
    id("io.quarkus")
}

group = "org.acme"

dependencies {
    implementation(enforcedPlatform("io.quarkus:quarkus-bom:${project.property("version")}"))
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")
    implementation(project(":lib"))
}
