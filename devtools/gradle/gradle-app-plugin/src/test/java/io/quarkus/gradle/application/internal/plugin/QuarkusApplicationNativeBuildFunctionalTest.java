package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;

import java.io.IOException;
import java.nio.file.Path;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.application.internal.nativeimage.NativeResult;
import io.quarkus.gradle.application.internal.nativeimage.NativeResultCodec;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.gradle.testing.BaseGradleTest;

class QuarkusApplicationNativeBuildFunctionalTest extends BaseGradleTest {

    private static final String NATIVE_SOURCES_TASK = ":quarkusSourcesBuild";

    @Test
    void namedNativeSourcesBuildUsesRealAugmentationWithoutRunningNativeImage() throws IOException {
        writeNativeSourcesApplication();

        BuildResult firstResult = buildResultWithIsolatedProjects(NATIVE_SOURCES_TASK);

        assertTaskOutcomes(firstResult, SUCCESS, NATIVE_SOURCES_TASK);
        assertNativeSourcesResult();

        BuildResult secondResult = buildResultWithIsolatedProjects(NATIVE_SOURCES_TASK);

        assertConfigurationCacheReused(secondResult);
        assertTaskOutcomes(secondResult, UP_TO_DATE, NATIVE_SOURCES_TASK);
        assertNativeSourcesResult();
    }

    private void assertNativeSourcesResult() {
        Path resultDirectory = testProjectDir.resolve("build/quarkus-build-results/sources/package");
        NativeResult result = new NativeResultCodec().read(resultDirectory.resolve("native-result.properties"));

        assertThat(result.buildName()).isEqualTo("sources");
        assertThat(result.buildType()).isEqualTo(QuarkusApplicationBuildType.NATIVE_SOURCES);
        assertThat(result.outputName()).isEqualTo("native-sources-application-999-SNAPSHOT");
        assertThat(result.outputRoot())
                .isEqualTo(testProjectDir.resolve("build/quarkus-builds/sources/package"));
        assertThat(result.executablePath()).isEmpty();
        assertThat(result.sourcesDirectory()).hasValueSatisfying(path -> assertThat(path).isDirectory());
        assertThat(result.sourceJarPath()).hasValueSatisfying(path -> {
            assertThat(path).doesNotExist();
            assertThat(result.sourcesDirectory().orElseThrow().resolve(path.getFileName())).isRegularFile();
        });
        assertThat(result.nativeImageArgsPath()).hasValueSatisfying(path -> assertThat(path).isRegularFile());
        assertThat(result.artifacts())
                .anySatisfy(artifact -> {
                    assertThat(artifact.type()).isEqualTo("native-sources");
                    assertThat(artifact.path()).isEqualTo(result.sourceJarPath());
                });
        assertThat(resultDirectory.resolve("native-augmentation-result.properties")).isRegularFile();
        assertThat(resultDirectory.resolve("quarkus-artifact.properties")).isRegularFile();
    }

    private void writeNativeSourcesApplication() throws IOException {
        writeFile("settings.gradle", "rootProject.name = 'native-sources-application'\n");
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

                quarkusApplication {
                    builds {
                        nativeSources('sources')
                    }
                }
                """);
        writeFile("src/main/java/org/acme/GreetingService.java", """
                package org.acme;

                import jakarta.enterprise.context.ApplicationScoped;

                @ApplicationScoped
                public class GreetingService {
                    public String greeting() {
                        return "hello";
                    }
                }
                """);
    }
}
