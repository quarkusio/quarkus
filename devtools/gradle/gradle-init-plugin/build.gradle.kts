plugins {
    id("io.quarkus.devtools.gradle-plugin")
}

group = "io.quarkus"

gradlePlugin {
    plugins.create("quarkusInitPlugin") {
        id = "io.quarkus.init"
        implementationClass = "io.quarkus.gradle.init.QuarkusInitSpecsPlugin"
        displayName = "Quarkus Init Plugin"
        description =
            "Registers a 'quarkus-app' build init spec so 'gradle init --type quarkus-app' can scaffold a Quarkus project without the Quarkus CLI"
        tags.addAll("quarkus", "quarkusio", "init")
    }
}

// RegistryClientTestHelper (used by tests to resolve the locally-built platform BOM
// instead of the live registry) reads these two system properties.
tasks.test {
    systemProperty("project.version", project.version.toString())
    systemProperty("project.groupId", project.group.toString())
}

// to generate reproducible jars
tasks.withType<Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
