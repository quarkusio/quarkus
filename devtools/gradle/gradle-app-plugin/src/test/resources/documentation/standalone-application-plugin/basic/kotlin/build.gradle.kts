// Source: {{source}}
// tag::plugin-application[]
plugins {
    id("io.quarkus.application")
}
// end::plugin-application[]

group = "org.acme"
version = "1.0.0"

// tag::minimal-named-build[]
quarkusApplication {
    builds {
        fastJar("production")
    }
}
// end::minimal-named-build[]

// tag::assemble-integration[]
quarkusApplication {
    builds {
        fastJar("assembled") {
            participatesInAssemble = true
        }
    }
}
// end::assemble-integration[]

// tag::package-customization[]
quarkusApplication {
    builds {
        fastJar("customized") {
            archiveBaseName = "acme-service"
            archiveBaseNameSuffix = "-prod"
            outputDirectory = layout.buildDirectory.dir("application")
            manifest {
                attributes.put("Implementation-Title", "Acme service")
                sections {
                    section("Specification") {
                        attributes.put("Specification-Version", "1.0")
                    }
                }
            }
        }
    }
}
// end::package-customization[]

check(tasks.names.containsAll(listOf(
    "quarkusProductionBuild",
    "quarkusProductionRun",
    "quarkusProductionShowEffectiveConfig"
)))
check(configurations.names.containsAll(listOf(
    "quarkusProductionPackageElements",
    "quarkusProductionLauncherJarElements"
)))
