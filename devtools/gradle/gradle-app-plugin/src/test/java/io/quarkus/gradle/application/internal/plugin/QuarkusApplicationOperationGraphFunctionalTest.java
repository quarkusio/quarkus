package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.testing.BaseGradleTest;

class QuarkusApplicationOperationGraphFunctionalTest extends BaseGradleTest {

    @Test
    void sameProjectJandexPrecedesNamedBuildAndExternalOperationTasks() throws IOException {
        writeApplication();

        List<String> taskPaths = List.of(
                ":quarkusFastBuild",
                ":quarkusFastImageBuild",
                ":quarkusFastDeployToDev",
                ":quarkusFastStartupOptimizedImageBuild");

        BuildResult firstResult = dryRun(taskPaths);
        for (String taskPath : taskPaths) {
            assertTasksOrdered(firstResult, ":jandex", ":quarkusApplicationModel", taskPath);
        }

        BuildResult secondResult = dryRun(taskPaths);
        assertThat(secondResult.getOutput()).contains("Configuration cache entry reused.");
        for (String taskPath : taskPaths) {
            assertTasksOrdered(secondResult, ":jandex", ":quarkusApplicationModel", taskPath);
        }
    }

    private BuildResult dryRun(List<String> taskPaths) {
        var arguments = new ArrayList<>(taskPaths);
        arguments.add("--dry-run");
        arguments.add(BUILD_CACHE);
        return buildResultWithIsolatedProjects(arguments.toArray(String[]::new));
    }

    private void writeApplication() throws IOException {
        writeFile("settings.gradle", "rootProject.name = 'operation-graph-app'\n");
        writeFile("gradle.properties", "version = 999-SNAPSHOT\n");
        writeFile("build.gradle", """
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

                def aotFile = layout.buildDirectory.file('aot/app.aot')

                tasks.register('jandex') {
                    def marker = layout.buildDirectory.file('jandex/jandex.marker')
                    outputs.file(marker)
                    doLast {
                        def output = marker.get().asFile
                        output.parentFile.mkdirs()
                        output.text = 'jandex'
                    }
                }

                quarkusApplication {
                    builds {
                        aotJar('fast', io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType.AOT) {
                            startupArchive {
                                it.file.set(aotFile)
                            }
                            startupOptimizedImage {
                            }
                            deployments {
                                kubernetes('dev')
                            }
                        }
                    }
                }
                """);
        writeFile("src/main/java/org/acme/GreetingService.java", """
                package org.acme;

                import jakarta.enterprise.context.ApplicationScoped;

                @ApplicationScoped
                public class GreetingService {
                    public String hello() {
                        return "hello";
                    }
                }
                """);
    }
}
