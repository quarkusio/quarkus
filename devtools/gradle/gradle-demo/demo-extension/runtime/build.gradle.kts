plugins {
    `java-library`
    id("io.quarkus.extension")
}

group = "org.acme.gradledemo"
version = "1.0.0"

val quarkusPlatformVersion = providers.gradleProperty("quarkusPlatformVersion").get()

quarkusExtension {
    deploymentModule = "deployment"
}

dependencies {
    implementation(enforcedPlatform("io.quarkus:quarkus-bom:$quarkusPlatformVersion"))
    implementation("io.quarkus:quarkus-arc")
}
