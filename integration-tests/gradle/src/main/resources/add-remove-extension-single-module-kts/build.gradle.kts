plugins {
    java
    id("io.quarkus")
}

repositories {
    if (System.getProperties().containsKey("maven.repo.local")) {
        maven(url = System.getProperties().get("maven.repo.local")!!)
    } else {
        mavenLocal()
    }
    mavenCentral()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-resteasy")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}

group  = "org.acme"
version = "1.0.0-SNAPSHOT"

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}
