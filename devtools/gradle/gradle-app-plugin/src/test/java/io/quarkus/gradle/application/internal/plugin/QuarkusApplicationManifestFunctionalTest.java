package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;

import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.application.internal.packaging.PackageResult;
import io.quarkus.gradle.application.internal.packaging.PackageResultCodec;
import io.quarkus.gradle.testing.BaseGradleTest;

class QuarkusApplicationManifestFunctionalTest extends BaseGradleTest {

    @Test
    void kotlinDslConfiguresEveryJarOutputWithConfigurationCacheAndIsolatedProjects() throws IOException {
        writeFile("settings.gradle.kts", "");
        writeFile("build.gradle.kts", """
                plugins {
                    id("io.quarkus.application")
                }

                version = "1.0"

                quarkusApplication {
                    builds {
                        fastJar("fast") {
                            manifest {
                                attributes.put("Built-By", "fast")
                                sections {
                                    section("Specification") {
                                        attributes.put("Specification-Title", "fast specification")
                                    }
                                }
                            }
                        }
                        mutableJar("mutable") {
                            manifest {
                                attributes.put("Built-By", "mutable")
                            }
                        }
                        aotJar("aot") {
                            manifest {
                                attributes.put("Built-By", "aot")
                            }
                        }
                        legacyJar("legacy") {
                            manifest {
                                attributes.put("Built-By", "legacy")
                            }
                        }
                        uberJar("uber") {
                            manifest {
                                attributes.put("Built-By", "uber")
                            }
                        }
                        nativeExecutable("native")
                        nativeSources("nativeSources")
                    }
                }
                """);

        String[] tasks = {
                "quarkusFastShowEffectiveConfig",
                "--show-values",
                "quarkusMutableShowEffectiveConfig",
                "--show-values",
                "quarkusAotShowEffectiveConfig",
                "--show-values",
                "quarkusLegacyShowEffectiveConfig",
                "--show-values",
                "quarkusUberShowEffectiveConfig",
                "--show-values"
        };
        BuildResult first = buildResultWithIsolatedProjects(tasks);
        assertTaskOutcomes(first, SUCCESS,
                ":quarkusFastShowEffectiveConfig",
                ":quarkusMutableShowEffectiveConfig",
                ":quarkusAotShowEffectiveConfig",
                ":quarkusLegacyShowEffectiveConfig",
                ":quarkusUberShowEffectiveConfig");
        assertThat(first.getOutput())
                .contains("quarkus.package.jar.manifest.attributes.\"Built-By\"=fast")
                .contains("quarkus.package.jar.manifest.attributes.\"Built-By\"=mutable")
                .contains("quarkus.package.jar.manifest.attributes.\"Built-By\"=aot")
                .contains("quarkus.package.jar.manifest.attributes.\"Built-By\"=legacy")
                .contains("quarkus.package.jar.manifest.attributes.\"Built-By\"=uber")
                .contains("quarkus.package.jar.manifest.sections.\"Specification\"."
                        + "\"Specification-Title\"=fast specification");

        BuildResult second = buildResultWithIsolatedProjects(tasks);
        assertConfigurationCacheReused(second);
    }

    @Test
    void groovyDslManifestOverridesBuildPropertiesAndTracksProviderChanges() throws IOException {
        writeTinyGroovyApplication();

        BuildResult first = buildResultWithIsolatedProjects(
                "quarkusAppBuild", "quarkusUberBuild", "-PmanifestVersion=1");
        assertTaskOutcomes(first, SUCCESS, ":quarkusAppBuild", ":quarkusUberBuild");
        assertManifest("app", "manifest-dsl", "Manifest App", "fixed", "1");
        assertManifest("uber", "uber-manifest-dsl", "Uber Manifest App", "fixed", "1");

        BuildResult second = buildResultWithIsolatedProjects(
                "quarkusAppBuild", "quarkusUberBuild", "-PmanifestVersion=1");
        assertConfigurationCacheReused(second);
        assertTaskOutcomes(second, UP_TO_DATE, ":quarkusAppBuild", ":quarkusUberBuild");

        BuildResult changed = buildResultWithIsolatedProjects(
                "quarkusAppBuild", "quarkusUberBuild", "-PmanifestVersion=2");
        assertTaskOutcomes(changed, SUCCESS, ":quarkusAppBuild", ":quarkusUberBuild");
        assertManifest("app", "manifest-dsl", "Manifest App", "fixed", "2");
        assertManifest("uber", "uber-manifest-dsl", "Uber Manifest App", "fixed", "2");
    }

    @Test
    void invalidManifestNamesFailWithNamedBuildAndTaskContext() throws IOException {
        writeInvalidManifestBuild("""
                manifest {
                    attributes.putAll(
                        providers.gradleProperty('manifestAttributeName').map {
                            [(it): 'value']
                        }
                    )
                }
                """);
        assertThat(prepareBuildWithIsolatedProjects(
                "quarkusBadShowEffectiveConfig", "-PmanifestAttributeName=invalid name")
                .buildAndFail().getOutput())
                .contains("Manifest attribute name 'invalid name'")
                .contains("named build 'bad'")
                .contains("task ':quarkusBadShowEffectiveConfig'");

        writeInvalidManifestBuild("""
                manifest {
                    sections {
                        section(' ') {
                            attributes.put('Built-By', 'value')
                        }
                    }
                }
                """);
        assertThat(prepareBuildWithIsolatedProjects("quarkusBadShowEffectiveConfig").buildAndFail().getOutput())
                .contains("Manifest section name")
                .contains("named build 'bad'")
                .contains("task ':quarkusBadShowEffectiveConfig'");

        writeInvalidManifestBuild("""
                manifest {
                    attributes.put('Built-By', 'first')
                    attributes.put('built-by', 'second')
                }
                """);
        assertThat(prepareBuildWithIsolatedProjects("quarkusBadShowEffectiveConfig").buildAndFail().getOutput())
                .contains("Manifest attribute names 'Built-By' and 'built-by'")
                .contains("differ only by case")
                .contains("named build 'bad'")
                .contains("task ':quarkusBadShowEffectiveConfig'");
    }

    private void writeTinyGroovyApplication() throws IOException {
        writeFile("settings.gradle", "rootProject.name = 'manifest-app'\n");
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
                    implementation 'io.quarkus:quarkus-arc'
                }

                quarkusApplication {
                    builds {
                        fastJar('app') {
                            quarkusBuildProperties.put(
                                'quarkus.package.jar.manifest.attributes."Built-By"',
                                'raw-build-property'
                            )
                            manifest {
                                attributes.put('Built-By', 'manifest-dsl')
                                attributes.put('Build-Version', 'fixed')
                                sections {
                                    section('Specification') {
                                        attributes.put('Specification-Title', 'Manifest App')
                                    }
                                    section('Build/Info') {
                                        attributes.put(
                                            'Build-Version',
                                            providers.gradleProperty('manifestVersion')
                                        )
                                    }
                                }
                            }
                        }
                        uberJar('uber') {
                            quarkusBuildProperties.put(
                                'quarkus.package.jar.manifest.attributes."Built-By"',
                                'raw-uber-build-property'
                            )
                            manifest {
                                attributes.put('Built-By', 'uber-manifest-dsl')
                                attributes.put('Build-Version', 'fixed')
                                sections {
                                    section('Specification') {
                                        attributes.put('Specification-Title', 'Uber Manifest App')
                                    }
                                    section('Build/Info') {
                                        attributes.put(
                                            'Build-Version',
                                            providers.gradleProperty('manifestVersion')
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                """);
        writeFile("src/main/java/org/acme/App.java", """
                package org.acme;

                public final class App {
                }
                """);
    }

    private void writeInvalidManifestBuild(String manifest) throws IOException {
        writeFile("settings.gradle", "");
        writeFile("build.gradle", """
                plugins {
                    id 'io.quarkus.application'
                }

                version = '1.0'

                quarkusApplication {
                    builds {
                        fastJar('bad') {
                %s
                        }
                    }
                }
                """.formatted(manifest.indent(12)));
    }

    private void assertManifest(String buildName, String builtBy, String specificationTitle, String mainBuildVersion,
            String sectionBuildVersion) throws IOException {
        PackageResult result = new PackageResultCodec().read(testProjectDir.resolve(
                Path.of("build", "quarkus-build-results", buildName, "package", "package-result.properties")));
        try (JarFile jar = new JarFile(result.jarPath().toFile())) {
            Manifest manifest = jar.getManifest();
            assertThat(manifest.getMainAttributes().getValue("Built-By")).isEqualTo(builtBy);
            assertThat(manifest.getMainAttributes().getValue("Build-Version")).isEqualTo(mainBuildVersion);
            assertThat(manifest.getEntries().get("Specification").getValue("Specification-Title"))
                    .isEqualTo(specificationTitle);
            assertThat(manifest.getEntries().get("Build/Info").getValue("Build-Version"))
                    .isEqualTo(sectionBuildVersion);
        }
    }
}
