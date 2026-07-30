// Source: {{source}}

plugins {
    id("io.quarkus.application")
}

version = "1.0.0"

val quarkusVersion = providers.gradleProperty("quarkusPluginVersion").get()

repositories {
    mavenCentral()
}

dependencies {
    implementation(enforcedPlatform(
        "io.quarkus.platform:quarkus-bom:$quarkusVersion"
    ))
    implementation("io.quarkus:quarkus-rest")
}

// tag::standard-and-custom-tests[]
val smokeTest by tasks.registering(org.gradle.api.tasks.testing.Test::class) {
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
}

quarkusApplication {
    tests {
        task(smokeTest)
    }
}
// end::standard-and-custom-tests[]

// tag::package-backed-tests[]
quarkusApplication {
    builds {
        fastJar("integration")
    }
}

testing {
    suites {
        register<org.gradle.api.plugins.jvm.JvmTestSuite>("integrationTest") {
            forQuarkusIntegrationTests("integration")
        }
    }
}
// end::package-backed-tests[]

// tag::native-tests[]
val defaultTests = testing.suites.named<org.gradle.api.plugins.jvm.JvmTestSuite>("test")

quarkusApplication {
    builds {
        nativeExecutable("native")
    }
}

testing.suites.named<org.gradle.api.plugins.jvm.JvmTestSuite>("quarkusNativeNativeTest") {
    includeTestsFrom(defaultTests)
}
// end::native-tests[]

check(tasks.names.containsAll(listOf(
    "test",
    "smokeTest",
    "integrationTest",
    "quarkusNativeNativeTest"
)))
