// Source: {{source}}
// tag::three-x-deployment-project[]
plugins {
    `java-library`
}
// end::three-x-deployment-project[]

group = "org.acme"
version = "1.0.0"

val quarkusPlatformVersion: String by project
dependencies {
    implementation(enforcedPlatform("io.quarkus:quarkus-bom:$quarkusPlatformVersion"))
    implementation(project(":runtime"))
    implementation("io.quarkus:quarkus-arc-deployment")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")
}
