package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.FAILED;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.application.internal.image.BuiltContainerImage;
import io.quarkus.gradle.application.internal.image.BuiltContainerImageResultCodec;
import io.quarkus.gradle.testing.BaseGradleTest;

class QuarkusApplicationImageActionFunctionalTest extends BaseGradleTest {

    private static final String IMAGE_REFERENCE = "quay.io/acme/synthetic:1.0";
    private static final String IMAGE_TASK = ":quarkusFastImageBuild";

    @Test
    void imageActionUsesSyntheticAugmentationResultAndAlwaysExecutes() throws IOException {
        writeApplicationWithSyntheticImageExtension();

        BuildResult firstResult = imageBuild();

        assertTaskOutcomes(firstResult, SUCCESS, IMAGE_TASK);
        assertSyntheticImageReceipt();

        BuildResult unchangedResult = imageBuild();

        assertTaskOutcomes(unchangedResult, SUCCESS, IMAGE_TASK);
        assertThat(unchangedResult.getOutput()).contains("Configuration cache entry reused.");
        assertSyntheticImageReceipt();

        Files.delete(imageReceipt());
        BuildResult missingReceiptResult = imageBuild();

        assertTaskOutcomes(missingReceiptResult, SUCCESS, IMAGE_TASK);
        assertThat(missingReceiptResult.getOutput()).contains("Configuration cache entry reused.");
        assertSyntheticImageReceipt();
    }

    @Test
    void kotlinImageActionRejectsReferenceCombinedWithTagAfterDslConfiguration() throws IOException {
        writeApplicationWithSyntheticImageExtension();
        Files.delete(testProjectDir.resolve("build.gradle"));
        writeFile("build.gradle.kts", """
                plugins {
                    id("io.quarkus.application")
                }

                group = "org.acme"
                version = "1.0"

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                dependencies {
                    implementation(enforcedPlatform("io.quarkus:quarkus-bom:999-SNAPSHOT"))
                    implementation(project(":probe-runtime"))
                }

                quarkusApplication {
                    builds {
                        fastJar("fast") {
                            image {
                                imageReference.set("quay.io/acme/synthetic:1.0")
                                tag.set("other")
                            }
                        }
                    }
                }
                """);

        BuildResult result = prepareBuildWithIsolatedProjects(IMAGE_TASK, BUILD_CACHE).buildAndFail();

        assertTaskOutcomes(result, FAILED, ":quarkusFastImageBuildReferencePreflight");
        assertThat(result.task(IMAGE_TASK)).isNull();
        assertThat(result.getOutput())
                .contains("Quarkus application image reference cannot be combined with repository or tag");
        assertThat(imageReceipt()).doesNotExist();
    }

    @Test
    void selectedNamedBuildsCannotClaimTheSameEffectiveImageReference() throws IOException {
        writeApplicationWithSyntheticImageExtension();
        appendSecondNamedBuild();

        BuildResult result = prepareBuildWithIsolatedProjects(
                IMAGE_TASK, ":quarkusOtherImageBuild", BUILD_CACHE, "--parallel").buildAndFail();

        assertThat(result.getOutput())
                .contains("Container image reference collision for '" + IMAGE_REFERENCE + "'")
                .contains("named build 'fast'")
                .contains("named build 'other'");
        assertThat(imageReceipt()).doesNotExist();
        assertThat(otherImageReceipt()).doesNotExist();
        assertThat(imageOperationMarker("fast")).doesNotExist();
        assertThat(imageOperationMarker("other")).doesNotExist();
    }

    private BuildResult imageBuild() {
        return buildResultWithIsolatedProjects(IMAGE_TASK, BUILD_CACHE);
    }

    private void assertSyntheticImageReceipt() {
        BuiltContainerImage image = new BuiltContainerImageResultCodec().read(imageReceipt());
        assertThat(image.resultType()).isEqualTo("jar-container");
        assertThat(image.builder()).isEmpty();
        assertThat(image.pushed()).isFalse();
        assertThat(image.reference()).contains(IMAGE_REFERENCE);
        assertThat(image.pullRequired()).contains(false);
        assertThat(image.workingDirectory()).contains("/synthetic-work");
        assertThat(image.outputDirectory()).contains("synthetic-output");
    }

    private Path imageReceipt() {
        return testProjectDir.resolve(
                "build/quarkus-build-results/fast/image-build/image-build-result.properties");
    }

    private Path otherImageReceipt() {
        return testProjectDir.resolve(
                "build/quarkus-build-results/other/image-build/image-build-result.properties");
    }

    private Path imageOperationMarker(String buildName) {
        return testProjectDir.resolve("build/quarkus-builds/" + buildName
                + "/image-build/synthetic-image-operation.marker");
    }

    private void appendSecondNamedBuild() throws IOException {
        Files.writeString(testProjectDir.resolve("build.gradle"), """

                quarkusApplication {
                    builds {
                        fastJar('other') {
                            image {
                                imageReference.set('quay.io/acme/synthetic:1.0')
                            }
                        }
                    }
                }
                """, StandardOpenOption.APPEND);
    }

    private void writeApplicationWithSyntheticImageExtension() throws IOException {
        writeApplicationWithSyntheticImageExtension(
                "imageReference.set('quay.io/acme/synthetic:1.0')");
    }

    private void writeApplicationWithSyntheticImageExtension(String imageConfiguration) throws IOException {
        writeFile("settings.gradle", """
                rootProject.name = 'synthetic-image-application'
                include 'probe-runtime', 'probe-deployment'
                """);
        writeFile("gradle.properties", "version=999-SNAPSHOT\n");
        writeFile("build.gradle", """
                plugins {
                    id 'io.quarkus.application'
                }

                group = 'org.acme'
                version = '1.0'

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                dependencies {
                    implementation enforcedPlatform('io.quarkus:quarkus-bom:999-SNAPSHOT')
                    implementation project(':probe-runtime')
                }

                quarkusApplication {
                    builds {
                        fastJar('fast') {
                            image {
                                %s
                            }
                        }
                    }
                }
                """.formatted(imageConfiguration));
        writeFile("src/main/java/org/acme/Application.java", """
                package org.acme;

                public final class Application {
                }
                """);
        writeSyntheticImageExtension();
    }

    private void writeSyntheticImageExtension() throws IOException {
        writeFile("probe-runtime/build.gradle", """
                plugins {
                    id 'java-library'
                    id 'io.quarkus.extension'
                }

                group = 'org.acme'
                version = '1.0'

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                quarkusExtension {
                    disableValidation = true
                    deploymentModule = 'probe-deployment'
                }

                dependencies {
                    implementation enforcedPlatform('io.quarkus:quarkus-bom:999-SNAPSHOT')
                    implementation 'io.quarkus:quarkus-core'
                }
                """);
        writeFile("probe-runtime/src/main/java/org/acme/probe/SyntheticImageExtension.java", """
                package org.acme.probe;

                public final class SyntheticImageExtension {
                }
                """);
        writeFile("probe-deployment/build.gradle", """
                plugins {
                    id 'java-library'
                    id 'io.quarkus.extension.deployment'
                }

                group = 'org.acme'
                version = '1.0'

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                dependencies {
                    implementation enforcedPlatform('io.quarkus:quarkus-bom:999-SNAPSHOT')
                    annotationProcessor 'io.quarkus:quarkus-extension-processor:999-SNAPSHOT'
                    implementation 'io.quarkus:quarkus-core-deployment'
                    implementation 'io.quarkus:quarkus-container-image-spi'
                }
                """);
        writeFile("probe-deployment/src/main/java/org/acme/probe/deployment/SyntheticImageProcessor.java", """
                package org.acme.probe.deployment;

                import java.io.IOException;
                import java.nio.file.Files;
                import java.util.Arrays;
                import java.util.List;
                import java.util.Map;
                import java.util.Optional;

                import org.eclipse.microprofile.config.ConfigProvider;

                import io.quarkus.container.spi.ContainerImageInfoBuildItem;
                import io.quarkus.deployment.annotations.BuildStep;
                import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
                import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;

                class SyntheticImageProcessor {

                    @BuildStep
                    ContainerImageInfoBuildItem imageInfo() {
                        String image = image();
                        int tagSeparator = image.lastIndexOf(':');
                        int registrySeparator = image.indexOf('/');
                        List<String> additionalTags = ConfigProvider.getConfig()
                                .getOptionalValue("quarkus.container-image.additional-tags", String.class)
                                .stream()
                                .flatMap(value -> Arrays.stream(value.split(",")))
                                .map(String::trim)
                                .filter(value -> !value.isEmpty())
                                .toList();
                        return new ContainerImageInfoBuildItem(
                                Optional.of(image.substring(0, registrySeparator)),
                                Optional.empty(), Optional.empty(),
                                image.substring(registrySeparator + 1, tagSeparator),
                                image.substring(tagSeparator + 1), additionalTags);
                    }

                    @BuildStep
                    ArtifactResultBuildItem syntheticContainerImage(OutputTargetBuildItem outputTarget) throws IOException {
                        Files.createDirectories(outputTarget.getOutputDirectory());
                        Files.writeString(
                                outputTarget.getOutputDirectory().resolve("synthetic-image-operation.marker"),
                                "executed");
                        return new ArtifactResultBuildItem(null, "jar-container", Map.of(
                                "container-image", image(),
                                "pull-required", "false",
                                "working-directory", "/synthetic-work",
                                "output-directory", "synthetic-output"));
                    }

                    private static String image() {
                        return ConfigProvider.getConfig()
                                .getValue("quarkus.container-image.image", String.class);
                    }
                }
                """);
    }
}
