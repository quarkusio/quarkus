plugins {
    `java-library`
    id("io.quarkus.extension.deployment")
}

group = "org.acme.gradledemo"
version = "1.0.0"

val quarkusPlatformVersion = providers.gradleProperty("quarkusPlatformVersion").get()

dependencies {
    implementation(enforcedPlatform("io.quarkus:quarkus-bom:$quarkusPlatformVersion"))
    implementation(project(":demo-greeting-extension"))
    implementation("io.quarkus:quarkus-arc-deployment")
}
