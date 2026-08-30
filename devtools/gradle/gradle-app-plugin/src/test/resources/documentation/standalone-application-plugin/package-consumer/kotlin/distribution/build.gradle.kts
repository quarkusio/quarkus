// Source: {{source}}
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.publish.maven.MavenPublication

plugins {
    base
    `maven-publish`
}

// tag::package-variants[]
val serverPackage by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val serverLauncher by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    serverPackage(project(
        path = ":app",
        configuration = "quarkusServerPackageElements"
    ))
    serverLauncher(project(
        path = ":app",
        configuration = "quarkusServerLauncherJarElements"
    ))
}
// end::package-variants[]

// tag::attribute-selected-package[]
val selectedServerPackage by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(
            org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE,
            objects.named(org.gradle.api.attributes.Category::class, "quarkus-application-package")
        )
        attribute(
            org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
            objects.named(org.gradle.api.attributes.LibraryElements::class, "quarkus-application-package-directory")
        )
        attribute(org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "directory")
        attribute(
            org.gradle.api.attributes.Attribute.of(
                "io.quarkus.application.build-name", String::class.java
            ),
            "server"
        )
        attribute(
            org.gradle.api.attributes.Attribute.of(
                "io.quarkus.application.build-type", String::class.java
            ),
            "fast-jar"
        )
    }
}

dependencies {
    selectedServerPackage(project(":app"))
}

tasks.register<Sync>("copySelectedServerPackage") {
    from(selectedServerPackage)
    into(layout.buildDirectory.dir("selected-server-package"))
}
// end::attribute-selected-package[]

// tag::archive-package[]
val packageDistribution by tasks.registering(Zip::class) {
    from(serverPackage)
    archiveBaseName = "acme-server"
    isZip64 = true
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.register<Tar>("packageDistributionTar") {
    from(serverPackage)
    archiveBaseName = "acme-server"
    compression = Compression.GZIP
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
// end::archive-package[]

// tag::publish-package[]
publishing {
    publications {
        create<MavenPublication>("server") {
            artifact(packageDistribution) {
                classifier = "distribution"
            }
        }
    }
}
// end::publish-package[]

check(tasks.names.containsAll(listOf(
    "copySelectedServerPackage",
    "packageDistribution",
    "packageDistributionTar",
    "generatePomFileForServerPublication"
)))
