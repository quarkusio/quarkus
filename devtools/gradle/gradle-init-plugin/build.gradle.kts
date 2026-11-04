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

// The functional test resolves the plugin the same way a real `gradle init` invocation
// does: from a Maven repository, by coordinates. Publish here instead of relying on
// mavenLocal so the test doesn't depend on, or pollute, the developer's real repository.
publishing {
    repositories {
        maven {
            name = "functionalTest"
            url = uri(layout.buildDirectory.dir("test-repo"))
        }
    }
}

// RegistryClientTestHelper (used by tests to resolve the locally-built platform BOM
// instead of the live registry) reads these two system properties.
tasks.test {
    dependsOn("publishAllPublicationsToFunctionalTestRepository")
    systemProperty("project.version", project.version.toString())
    systemProperty("project.groupId", project.group.toString())
    systemProperty("functionalTest.repo", layout.buildDirectory.dir("test-repo").get().asFile.toURI().toString())
}

// to generate reproducible jars
tasks.withType<Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
