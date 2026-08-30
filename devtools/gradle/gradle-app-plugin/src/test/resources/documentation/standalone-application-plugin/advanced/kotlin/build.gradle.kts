// Source: {{source}}

plugins {
    id("io.quarkus.application")
}

group = "org.acme"
version = "1.0.0"

repositories {
    mavenCentral()
}

// tag::development-dependencies[]
dependencies {
    quarkusDev("org.acme:development-helper:1.0")
}
// end::development-dependencies[]

// tag::dev-and-continuous-test[]
quarkusApplication {
    dev {
        continuousTesting = true
        environmentVariables.put("QUARKUS_LOG_LEVEL", "DEBUG")
        debug = true
        debugPort = 5005
        forkOptions {
            jvmArgs("-XX:+UseG1GC")
            systemProperty("quarkus.console.color", "false")
        }
    }
}
// end::dev-and-continuous-test[]

// tag::remote-development[]
quarkusApplication {
    builds {
        mutableJar("remote")
    }
    remoteDev {
        quarkusBuildProperties.put(
            "quarkus.live-reload.url",
            "http://localhost:8080"
        )
        forkOptions {
            jvmArgs("-XX:+UseG1GC")
        }
    }
}
// end::remote-development[]

// tag::offline-preparation[]
quarkusApplication {
    builds {
        fastJar("offline") {
            prepareForOffline = true
        }
    }
}
// end::offline-preparation[]

// tag::configuration-inputs[]
quarkusApplication {
    configInputs {
        projectProperties {
            prefixes.add("quarkus.")
            names.add("deploymentTarget")
        }
        systemProperties {
            prefixes.add("quarkus.")
        }
        environmentVariables {
            names.add("CI")
        }
    }
}
// end::configuration-inputs[]

// tag::fork-options[]
quarkusApplication {
    buildForkOptions {
        minHeapSize = "256m"
        maxHeapSize = "2g"
        enableAssertions = true
        systemProperty("user.language", "en")
        environment("QUARKUS_ANALYTICS_DISABLED", "true")
    }
    codeGenForkOptions {
        maxHeapSize = "1g"
    }
}
// end::fork-options[]

// tag::code-generation[]
quarkusApplication {
    codegen {
        providers.add("openapi")
        inputNames.add("openapi")
    }
}
// end::code-generation[]

// tag::images-and-deployments[]
quarkusApplication {
    builds {
        fastJar("image") {
            image {
                repository = "quay.io/acme/service"
                builder = io.quarkus.gradle.application.model.QuarkusApplicationImageBuilder.JIB
            }
            deployments {
                kubernetes("production")
            }
        }
    }
}
// end::images-and-deployments[]

// tag::startup-archives[]
quarkusApplication {
    builds {
        aotJar(
            "optimized",
            io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType.APP_CDS
        ) {
            startupArchive {
                fromPackageBuild()
            }
            startupOptimizedImage {
                imageSuffix = "-optimized"
            }
        }
    }
}
// end::startup-archives[]

// tag::startup-archive-training[]
quarkusApplication {
    builds {
        aotJar(
            "trained",
            io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType.AOT
        )
    }
}

testing {
    suites {
        register<org.gradle.api.plugins.jvm.JvmTestSuite>("training") {
            forQuarkusIntegrationTests("trained")
            startupArchiveTraining {
                executionTarget = io.quarkus.gradle.application.model
                    .QuarkusApplicationStartupArchiveTrainingExecutionTarget.HOST_JVM
            }
        }
    }
}
// end::startup-archive-training[]

// tag::diagnostics[]
quarkusApplication {
    builds {
        fastJar("diagnostics")
    }
}

tasks.register("quarkusDiagnostics") {
    dependsOn(
        "quarkusApplicationShowModel",
        "quarkusDiagnosticsShowEffectiveConfig"
    )
}
// end::diagnostics[]

check(tasks.names.containsAll(listOf(
    "quarkusApplicationDev",
    "quarkusApplicationContinuousTest",
    "quarkusApplicationRemoteDev",
    "quarkusRemoteRun",
    "quarkusApplicationPrepareOffline",
    "quarkusImageImageBuild",
    "quarkusImageDeployToProduction",
    "quarkusOptimizedStartupOptimizedImageBuild",
    "quarkusDiagnostics"
)))
