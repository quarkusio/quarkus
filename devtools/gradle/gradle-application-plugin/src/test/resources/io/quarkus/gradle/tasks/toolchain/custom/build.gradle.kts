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
    implementation("io.quarkus:quarkus-core")
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.withType(io.quarkus.gradle.tasks.QuarkusTask::class.java) {
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(21)
    }

}