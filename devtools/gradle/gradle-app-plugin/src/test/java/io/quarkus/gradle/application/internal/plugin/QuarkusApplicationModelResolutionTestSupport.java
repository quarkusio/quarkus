package io.quarkus.gradle.application.internal.plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import io.quarkus.gradle.testing.BaseGradleTest;

abstract class QuarkusApplicationModelResolutionTestSupport extends BaseGradleTest {

    final void writeLocalExtensionApplication() throws IOException {
        writeLocalExtensionApplication("");
    }

    final void writeLocalExtensionApplication(String extensionConfiguration) throws IOException {
        writeLocalExtensionApplication(extensionConfiguration, "", "");
    }

    final void writeLocalExtensionApplication(String extensionConfiguration, String runtimeBuildConfiguration,
            String applicationDependencies) throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle"), """
                rootProject.name = 'local-extension-application'
                include 'app', 'runtime-ext', 'deployment-ext'
                """);
        writeFile(testProjectDir.resolve("app/build.gradle"), """
                plugins {
                    id 'java'
                    id 'io.quarkus.application'
                }

                version = '1.0'

                repositories {
                    maven {
                        url = uri('../repo')
                    }
                }

                dependencies {
                    implementation project(':runtime-ext')
                    %s
                }

                tasks.register('resolveDeploymentClasspath') {
                    def deploymentClasspath = configurations.named('quarkusApplicationDeploymentClasspathConfiguration')
                    inputs.files(deploymentClasspath)
                    doLast {
                        def files = deploymentClasspath.get().files
                        assert files*.name.contains('deployment-ext-1.0.jar')
                        files*.name.sort().each { println "deploymentFile=${it}" }
                    }
                }

                tasks.register('resolveRuntimeClasspath') {
                    def runtimeClasspath = configurations.named('quarkusApplicationRuntimeClasspathConfiguration')
                    inputs.files(runtimeClasspath)
                    doLast {
                        runtimeClasspath.get().files*.name.sort().each { println "runtimeFile=${it}" }
                    }
                }

                tasks.register('resolveDevRuntimeClasspath') {
                    def runtimeClasspath = configurations.named('quarkusApplicationDevRuntimeClasspathConfiguration')
                    inputs.files(runtimeClasspath)
                    doLast {
                        runtimeClasspath.get().files*.name.sort().each { println "devRuntimeFile=${it}" }
                    }
                }

                tasks.register('resolveContinuousTestRuntimeClasspath') {
                    def runtimeClasspath =
                        configurations.named('quarkusApplicationContinuousTestRuntimeClasspathConfiguration')
                    inputs.files(runtimeClasspath)
                    doLast {
                        runtimeClasspath.get().files*.name.sort().each {
                            println "continuousTestRuntimeFile=${it}"
                        }
                    }
                }
                """.formatted(applicationDependencies));
        writeFile(testProjectDir.resolve("runtime-ext/build.gradle"), """
                plugins {
                    id 'java'
                    id 'io.quarkus.extension'
                }

                group = 'org.acme'
                version = '1.0'

                quarkusExtension {
                    disableValidation = true
                    deploymentModule = 'deployment-ext'
                    %s
                }

                %s
                """.formatted(extensionConfiguration, runtimeBuildConfiguration));
        writeFile(testProjectDir.resolve("runtime-ext/src/main/java/org/acme/runtime/RuntimeExtension.java"), """
                package org.acme.runtime;

                public final class RuntimeExtension {
                }
                """);
        writeFile(testProjectDir.resolve("deployment-ext/build.gradle"), """
                plugins {
                    id 'java-library'
                }

                group = 'org.acme'
                version = '1.0'
                """);
        writeFile(testProjectDir.resolve("deployment-ext/src/main/java/org/acme/deployment/DeploymentExtension.java"), """
                package org.acme.deployment;

                public final class DeploymentExtension {
                }
                """);
    }

    static void writeSyntheticConditionalExtensionRepository(Path repository) throws IOException {
        writeMavenArtifact(repository, "org.acme", "parent-extension", "1.0",
                """
                        conditional-dependencies=org.acme\\:satisfied-extension\\:\\:jar\\:1.0 org.acme\\:missing-extension\\:\\:jar\\:1.0
                        deployment-artifact=org.acme\\:parent-extension-deployment\\:1.0
                        """);
        writeMavenArtifact(repository, "org.condition", "present", "1.0", null);
        writeMavenArtifact(repository, "org.acme", "satisfied-extension", "1.0", """
                dependency-condition=org.condition\\:present
                deployment-artifact=org.acme\\:satisfied-extension-deployment\\:1.0
                """);
        writeMavenArtifact(repository, "org.acme", "missing-extension", "1.0", """
                dependency-condition=org.condition\\:missing
                deployment-artifact=org.acme\\:missing-extension-deployment\\:1.0
                """);
    }

    static void writeMavenArtifact(Path repository, String groupId, String artifactId, String version,
            String extensionDescriptor) throws IOException {
        Path artifactDirectory = repository.resolve(groupId.replace('.', '/')).resolve(artifactId).resolve(version);
        Files.createDirectories(artifactDirectory);
        String baseName = artifactId + "-" + version;
        writeFile(artifactDirectory.resolve(baseName + ".pom"), """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>%s</artifactId>
                  <version>%s</version>
                </project>
                """.formatted(groupId, artifactId, version));
        Path jarFile = artifactDirectory.resolve(baseName + ".jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarFile))) {
            if (extensionDescriptor != null) {
                jar.putNextEntry(new JarEntry("META-INF/quarkus-extension.properties"));
                jar.write(extensionDescriptor.getBytes(StandardCharsets.UTF_8));
                jar.closeEntry();
            }
        }
    }
}
