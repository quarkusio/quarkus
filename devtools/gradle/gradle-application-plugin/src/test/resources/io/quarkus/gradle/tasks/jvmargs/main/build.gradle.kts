plugins {
    java
    id("io.quarkus")
}

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(enforcedPlatform("io.quarkus:quarkus-bom:${project.property("version")}"))
    implementation("io.quarkus:quarkus-arc")
    testImplementation("io.quarkus:quarkus-junit5")
}

