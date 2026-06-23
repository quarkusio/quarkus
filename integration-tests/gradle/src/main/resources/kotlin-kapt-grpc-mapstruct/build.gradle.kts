plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    id("io.quarkus")
    kotlin("kapt")
}

repositories {
    mavenLocal {
        content {
            includeGroupByRegex("io.quarkus.*")
            includeGroup("org.hibernate.orm")
        }
    }
    mavenCentral()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project
val mapstructVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-grpc")
    implementation("com.google.protobuf:protobuf-kotlin")
    implementation("org.mapstruct:mapstruct:${mapstructVersion}")
    kapt("org.mapstruct:mapstruct-processor:${mapstructVersion}")
}

group = "org.acme"
version = "1.0.0-SNAPSHOT"

allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

kotlin {
    compilerOptions {
        javaParameters = true
    }
}
