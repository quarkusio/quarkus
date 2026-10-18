import java.util.Locale
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    id("io.quarkus.devtools.java-library")
}

dependencies {
    compileOnly(libs.kotlin.gradle.plugin.api)
    implementation("org.apache.maven:maven-core")
    gradleApi()
}

group = "io.quarkus"

java {
    withSourcesJar()
    withJavadocJar()
}

val generateGradleVersionSupport = tasks.register<GenerateGradleVersionSupport>("generateGradleVersionSupport") {
    buildParentPom.set(layout.projectDirectory.file("../../../build-parent/pom.xml"))
    outputDirectory.set(layout.buildDirectory.dir("generated/sources/gradle-version-support/java/main"))
}

sourceSets.named("main") {
    java.srcDir(generateGradleVersionSupport)
}

// to generate reproducible jars
tasks.withType<Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

publishing {
    publications.create<MavenPublication>("maven") {
        artifactId = "quarkus-gradle-model"
        from(components["java"])
    }
}

@CacheableTask
abstract class GenerateGradleVersionSupport : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val buildParentPom: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val properties = readProperties()
        val minimumGradleVersion = required(properties, "minimum-gradle-version")
        val supportedGradleVersions = required(properties, "supported-gradle-versions")
        val packageDirectory = outputDirectory.dir("io/quarkus/gradle").get().asFile
        packageDirectory.mkdirs()
        packageDirectory.resolve("GeneratedGradleVersionSupport.java").writeText(
            """
            package io.quarkus.gradle;

            final class GeneratedGradleVersionSupport {

                static final String MINIMUM_GRADLE_VERSION = "${javaString(minimumGradleVersion)}";
                static final String SUPPORTED_GRADLE_VERSIONS = "${javaString(supportedGradleVersions)}";

                private GeneratedGradleVersionSupport() {
                }
            }
            """.trimIndent() + "\n"
        )
    }

    private fun readProperties(): Map<String, String> {
        val factory = DocumentBuilderFactory.newInstance()
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        factory.isNamespaceAware = true
        val document = factory.newDocumentBuilder().parse(buildParentPom.get().asFile)
        val nodes = document.getElementsByTagNameNS("*", "properties")
        val result = linkedMapOf<String, String>()
        for (i in 0 until nodes.length) {
            val properties = nodes.item(i).childNodes
            for (j in 0 until properties.length) {
                val property = properties.item(j)
                val name = property.localName ?: property.nodeName
                val value = property.textContent?.trim()
                if (name != null && !value.isNullOrEmpty()) {
                    result[name] = value
                }
            }
        }
        return result
    }

    private fun required(properties: Map<String, String>, name: String): String =
        properties[name] ?: throw GradleException(
            "Required property '$name' is missing from ${buildParentPom.get().asFile}"
        )

    private fun javaString(value: String): String =
        value.flatMap { character ->
            when (character) {
                '\\' -> "\\\\".toList()
                '"' -> "\\\"".toList()
                '\n' -> "\\n".toList()
                '\r' -> "\\r".toList()
                '\t' -> "\\t".toList()
                else -> if (character.code < 0x20) {
                    "\\u%04x".format(Locale.ROOT, character.code).toList()
                } else {
                    listOf(character)
                }
            }
        }.joinToString("")
}
