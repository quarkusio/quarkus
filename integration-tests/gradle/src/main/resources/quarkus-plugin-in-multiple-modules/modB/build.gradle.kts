plugins {
    `java-library`
    id("io.quarkus")
    id("com.github.ben-manes.versions") version "0.51.0"
}
val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

val javaVersion = "17"

dependencies {
    implementation(enforcedPlatform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))
    implementation(project(":modA"))
}
