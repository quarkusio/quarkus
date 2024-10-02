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
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")
}

quarkus {
    quarkusBuildProperties.put("quarkus.foo", "bar")
    manifest {
        attributes(mapOf("Manifest-Attribute" to "some-value"))
    }

    // The following line is replaced by the tests in `TasksConfigurationCacheCompatibilityTest`
    // ADDITIONAL_CONFIG
}
