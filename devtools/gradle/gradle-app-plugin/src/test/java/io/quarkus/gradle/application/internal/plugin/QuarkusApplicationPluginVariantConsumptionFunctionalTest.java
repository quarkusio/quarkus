package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;

import java.io.IOException;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.testing.BaseGradleTest;

class QuarkusApplicationPluginVariantConsumptionFunctionalTest extends BaseGradleTest {

    @Test
    void packageAndLauncherVariantsShareProducerAndExposeDistinctPayloads() throws IOException {
        writePackageVariantProducerConsumerApplication();

        BuildResult result = buildResultWithIsolatedProjects(":consumer:verifyServer");

        assertTaskOutcomes(result, SUCCESS, ":app:quarkusFastBuild", ":consumer:copyServerPackage",
                ":consumer:verifyServer");
        assertThat(result.getOutput())
                .contains("packageDirectory=package")
                .contains("launcherFile=quarkus-run.jar");
        assertThat(testProjectDir.resolve("consumer/build/server-package/quarkus-run.jar")).isRegularFile();

        BuildResult second = buildResultWithIsolatedProjects(":consumer:verifyServer");

        assertConfigurationCacheReused(second);
        assertTaskOutcomes(second, UP_TO_DATE, ":app:quarkusFastBuild", ":consumer:copyServerPackage");
        assertTaskOutcomes(second, SUCCESS, ":consumer:verifyServer");
    }

    private void writePackageVariantProducerConsumerApplication() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle"), """
                rootProject.name = 'package-variant-consumer'
                include 'app', 'consumer'
                """);
        writeFile(testProjectDir.resolve("gradle.properties"), "version = 999-SNAPSHOT\n");
        writeFile(testProjectDir.resolve("app/build.gradle"), """
                plugins {
                    id 'io.quarkus.application'
                }

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                dependencies {
                    implementation enforcedPlatform("io.quarkus:quarkus-bom:${project.property('version')}")
                    implementation "io.quarkus:quarkus-arc"
                }

                quarkusApplication {
                    builds {
                        fastJar('fast')
                    }
                }
                """);
        writeFile(testProjectDir.resolve("app/src/main/java/org/acme/GreetingService.java"), """
                package org.acme;

                import jakarta.enterprise.context.ApplicationScoped;

                @ApplicationScoped
                public class GreetingService {
                    public String greeting() {
                        return "hello";
                    }
                }
                """);
        writeFile(testProjectDir.resolve("consumer/build.gradle"), """
                import org.gradle.api.DefaultTask
                import org.gradle.api.artifacts.type.ArtifactTypeDefinition
                import org.gradle.api.attributes.Attribute
                import org.gradle.api.attributes.Category
                import org.gradle.api.attributes.LibraryElements
                import org.gradle.api.file.ConfigurableFileCollection
                import org.gradle.api.file.DirectoryProperty
                import org.gradle.api.tasks.InputDirectory
                import org.gradle.api.tasks.InputFiles
                import org.gradle.api.tasks.TaskAction

                plugins {
                    id 'base'
                }

                configurations {
                    packageRoot {
                        canBeConsumed = false
                        canBeResolved = true
                        attributes {
                            attribute(Category.CATEGORY_ATTRIBUTE,
                                objects.named(Category, 'quarkus-application-package'))
                            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                                objects.named(LibraryElements, 'quarkus-application-package-directory'))
                            attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, 'directory')
                            attribute(Attribute.of('io.quarkus.application.build-name', String), 'fast')
                            attribute(Attribute.of('io.quarkus.application.build-type', String), 'fast-jar')
                        }
                    }
                    launcher {
                        canBeConsumed = false
                        canBeResolved = true
                        attributes {
                            attribute(Category.CATEGORY_ATTRIBUTE,
                                objects.named(Category, 'quarkus-application-launcher'))
                            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                                objects.named(LibraryElements, 'quarkus-application-launcher-jar'))
                            attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, 'jar')
                            attribute(Attribute.of('io.quarkus.application.build-name', String), 'fast')
                            attribute(Attribute.of('io.quarkus.application.build-type', String), 'fast-jar')
                        }
                    }
                }

                dependencies {
                    packageRoot project(':app')
                    launcher project(':app')
                }

                tasks.register('copyServerPackage', Sync) {
                    from(configurations.packageRoot)
                    into(layout.buildDirectory.dir('server-package'))
                }

                abstract class VerifyServer extends DefaultTask {
                    @InputFiles
                    abstract ConfigurableFileCollection getPackageRoots()

                    @InputFiles
                    abstract ConfigurableFileCollection getLauncherFiles()

                    @InputDirectory
                    abstract DirectoryProperty getCopiedPackage()

                    @TaskAction
                    void verify() {
                        def roots = packageRoots.files
                        def launchers = launcherFiles.files
                        assert roots.size() == 1
                        assert roots.first().isDirectory()
                        assert roots.first().toPath().resolve('quarkus-run.jar').toFile().isFile()
                        assert launchers*.name == ['quarkus-run.jar']
                        assert launchers.first().isFile()
                        assert launchers.first().toPath().startsWith(roots.first().toPath())
                        assert copiedPackage.file('quarkus-run.jar').get().asFile.isFile()
                        println "packageDirectory=${roots.first().name}"
                        println "launcherFile=${launchers.first().name}"
                    }
                }

                tasks.register('verifyServer', VerifyServer) {
                    dependsOn tasks.named('copyServerPackage')
                    packageRoots.from(configurations.packageRoot)
                    launcherFiles.from(configurations.launcher)
                    copiedPackage.set(layout.buildDirectory.dir('server-package'))
                }
                """);
    }

}
