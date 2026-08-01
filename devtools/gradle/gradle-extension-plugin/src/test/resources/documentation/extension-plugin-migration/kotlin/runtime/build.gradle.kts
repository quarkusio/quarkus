// Source: {{source}}
// tag::runtime-project[]
plugins {
    `java-library`
    id("io.quarkus.extension")
}

quarkusExtension {
    // Optional here because "deployment" is the convention.
    deploymentModule = "deployment"
}
// end::runtime-project[]

group = "org.acme"
version = "1.0.0"

val quarkusPlatformVersion: String by project
dependencies {
    implementation(enforcedPlatform("io.quarkus:quarkus-bom:$quarkusPlatformVersion"))
    implementation("io.quarkus:quarkus-arc")
}
