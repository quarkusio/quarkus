package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;

import java.io.IOException;
import java.nio.file.Path;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.application.internal.image.BuiltContainerImage;
import io.quarkus.gradle.application.internal.image.BuiltContainerImageResultCodec;
import io.quarkus.gradle.testing.BaseGradleTest;

class QuarkusApplicationStartupArchiveFunctionalTest extends BaseGradleTest {

    private static final String VERIFY_ARCHIVES_TASK = ":verifyStartupArchives";
    private static final String PRODUCE_SCC_TASK = ":produceScc";

    @Test
    void wiresExternalAotFileAndTaskProducedSccDirectoryIntoIndependentOutputs() throws IOException {
        writeExternalAndTaskProducedArchiveApplication();

        BuildResult firstResult = buildResultWithIsolatedProjects(VERIFY_ARCHIVES_TASK, BUILD_CACHE);

        assertThat(firstResult.task(PRODUCE_SCC_TASK).getOutcome()).isIn(SUCCESS, FROM_CACHE);
        assertThat(firstResult.task(VERIFY_ARCHIVES_TASK).getOutcome()).isIn(SUCCESS, FROM_CACHE);
        assertArchiveVerificationReport();
        assertThat(firstResult.task(":quarkusExternalAotStartupOptimizedImageBuild")).isNull();
        assertThat(firstResult.task(":quarkusGeneratedSccStartupOptimizedImageBuild")).isNull();

        BuildResult secondResult = buildResultWithIsolatedProjects(VERIFY_ARCHIVES_TASK, BUILD_CACHE);

        assertConfigurationCacheReused(secondResult);
        assertTaskOutcomes(secondResult, UP_TO_DATE, PRODUCE_SCC_TASK, VERIFY_ARCHIVES_TASK);
        assertArchiveVerificationReport();
    }

    @Test
    void packageProducedArchiveCarriesTypedConfigurationAndTaskDependency() throws IOException {
        writePackageProducedArchiveApplication();

        BuildResult firstResult = packagedOptimizedImageDryRun();

        assertTasksOrdered(firstResult,
                ":quarkusPackagedBuild",
                ":quarkusPackagedStartupOptimizedImageBuild");
        assertTasksOrdered(firstResult,
                ":quarkusPackagedImageBuild",
                ":quarkusPackagedStartupOptimizedImageBuild");

        BuildResult secondResult = packagedOptimizedImageDryRun();

        assertConfigurationCacheReused(secondResult);
        assertTasksOrdered(secondResult,
                ":quarkusPackagedBuild",
                ":quarkusPackagedStartupOptimizedImageBuild");
        assertTasksOrdered(secondResult,
                ":quarkusPackagedImageBuild",
                ":quarkusPackagedStartupOptimizedImageBuild");
    }

    @Test
    void ordinaryImageForArchiveFreeAotJarExplicitlyDisablesArchiveGeneration() throws IOException {
        writeArchiveFreeAotJarWithSyntheticImageExtension();

        BuildResult firstResult = ordinaryImageBuild();

        assertTaskOutcomes(firstResult, SUCCESS, ":quarkusOrdinaryImageBuild");
        assertSyntheticImageReceipt();

        BuildResult secondResult = ordinaryImageBuild();

        assertConfigurationCacheReused(secondResult);
        assertTaskOutcomes(secondResult, SUCCESS, ":quarkusOrdinaryImageBuild");
        assertSyntheticImageReceipt();
    }

    private BuildResult packagedOptimizedImageDryRun() {
        return buildResultWithIsolatedProjects(
                ":quarkusPackagedStartupOptimizedImageBuild", "--dry-run", BUILD_CACHE);
    }

    private BuildResult ordinaryImageBuild() {
        return buildResultWithIsolatedProjects(":quarkusOrdinaryImageBuild", BUILD_CACHE);
    }

    private void assertArchiveVerificationReport() {
        assertThat(testProjectDir.resolve("build/verification/startup-archives.txt"))
                .hasContent("""
                        external=AOT:app.aot:-aot:external-aot
                        generated=SCC:generated-scc:-trained-scc:generated-scc
                        """);
    }

    private void assertSyntheticImageReceipt() {
        Path receipt = testProjectDir.resolve(
                "build/quarkus-build-results/ordinary/image-build/image-build-result.properties");
        BuiltContainerImage image = new BuiltContainerImageResultCodec().read(receipt);
        assertThat(image.resultType()).isEqualTo("jar-container");
        assertThat(image.reference()).contains("quay.io/acme/archive-free:1.0");
        assertThat(image.workingDirectory()).contains("/synthetic-work");
    }

    private void writeExternalAndTaskProducedArchiveApplication() throws IOException {
        writeFile("settings.gradle", "rootProject.name = 'startup-archive-sources'\n");
        writeFile("archives/app.aot", "external-aot\n");
        writeFile("build.gradle", """
                import io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType
                import io.quarkus.gradle.application.tasks.QuarkusApplicationStartupOptimizedImageBuildTask
                import org.gradle.api.DefaultTask
                import org.gradle.api.file.DirectoryProperty
                import org.gradle.api.file.RegularFileProperty
                import org.gradle.api.provider.ListProperty
                import org.gradle.api.tasks.CacheableTask
                import org.gradle.api.tasks.Input
                import org.gradle.api.tasks.InputDirectory
                import org.gradle.api.tasks.InputFile
                import org.gradle.api.tasks.OutputDirectory
                import org.gradle.api.tasks.OutputFile
                import org.gradle.api.tasks.PathSensitive
                import org.gradle.api.tasks.PathSensitivity
                import org.gradle.api.tasks.TaskAction

                plugins {
                    id 'io.quarkus.application'
                }

                @CacheableTask
                abstract class ProduceScc extends DefaultTask {
                    @OutputDirectory
                    abstract DirectoryProperty getOutputDirectory()

                    @TaskAction
                    void produce() {
                        def output = outputDirectory.get().asFile
                        output.mkdirs()
                        new File(output, 'cache.layer').text = 'generated-scc\\n'
                    }
                }

                @CacheableTask
                abstract class VerifyStartupArchives extends DefaultTask {
                    @InputFile
                    @PathSensitive(PathSensitivity.RELATIVE)
                    abstract RegularFileProperty getExternalAot()

                    @InputDirectory
                    @PathSensitive(PathSensitivity.RELATIVE)
                    abstract DirectoryProperty getGeneratedScc()

                    @Input
                    abstract ListProperty<String> getArchiveDetails()

                    @OutputFile
                    abstract RegularFileProperty getReportFile()

                    @TaskAction
                    void verify() {
                        if (externalAot.get().asFile.text.trim() != 'external-aot') {
                            throw new GradleException('Unexpected external AOT archive')
                        }
                        def sccEntry = new File(generatedScc.get().asFile, 'cache.layer')
                        if (sccEntry.text.trim() != 'generated-scc') {
                            throw new GradleException('Unexpected generated SCC archive')
                        }
                        def report = reportFile.get().asFile
                        report.parentFile.mkdirs()
                        report.text = archiveDetails.get().join('\\n') + '\\n'
                    }
                }

                def sccProducer = tasks.register('produceScc', ProduceScc) {
                    outputDirectory.set(layout.buildDirectory.dir('startup-archives/generated-scc'))
                }

                quarkusApplication {
                    builds {
                        aotJar('external-aot', QuarkusApplicationJvmStartupArchiveType.AOT) {
                            startupArchive {
                                file.set(layout.projectDirectory.file('archives/app.aot'))
                            }
                            startupOptimizedImage {
                            }
                        }
                        aotJar('generated-scc', QuarkusApplicationJvmStartupArchiveType.SCC) {
                            startupArchive {
                                directory.set(sccProducer.flatMap { it.outputDirectory })
                            }
                            startupOptimizedImage {
                                imageSuffix.set('-trained-scc')
                            }
                        }
                    }
                }

                def externalImage = tasks.named(
                    'quarkusExternalAotStartupOptimizedImageBuild',
                    QuarkusApplicationStartupOptimizedImageBuildTask
                ).get()
                def generatedImage = tasks.named(
                    'quarkusGeneratedSccStartupOptimizedImageBuild',
                    QuarkusApplicationStartupOptimizedImageBuildTask
                ).get()

                tasks.register('verifyStartupArchives', VerifyStartupArchives) {
                    externalAot.set(externalImage.archiveFile)
                    generatedScc.set(generatedImage.archiveDirectory)
                    archiveDetails.add(externalImage.archiveType.zip(
                        externalImage.archiveFile.zip(externalImage.imageSuffix) { archive, suffix ->
                            "${archive.asFile.name}:${suffix}"
                        }
                    ) { type, archiveAndSuffix ->
                        "external=${type.name()}:${archiveAndSuffix}:external-aot"
                    })
                    archiveDetails.add(generatedImage.archiveType.zip(
                        generatedImage.archiveDirectory.zip(generatedImage.imageSuffix) { archive, suffix ->
                            "${archive.asFile.name}:${suffix}"
                        }
                    ) { type, archiveAndSuffix ->
                        "generated=${type.name()}:${archiveAndSuffix}:generated-scc"
                    })
                    reportFile.set(layout.buildDirectory.file('verification/startup-archives.txt'))
                }
                """);
    }

    private void writePackageProducedArchiveApplication() throws IOException {
        writeFile("settings.gradle", "rootProject.name = 'package-produced-startup-archive'\n");
        writeFile("build.gradle", """
                import io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType
                import io.quarkus.gradle.application.tasks.QuarkusApplicationPackageTask

                plugins {
                    id 'io.quarkus.application'
                }

                version = '1.0'

                def packagedOutput = quarkusApplication.builds.aotJar(
                    'packaged',
                    QuarkusApplicationJvmStartupArchiveType.AOT
                ) {
                    startupArchive {
                        fromPackageBuild()
                    }
                    startupOptimizedImage {
                    }
                }

                def packageTask = tasks.named('quarkusPackagedBuild', QuarkusApplicationPackageTask).get()
                def archive = packagedOutput.get().startupArchive
                assert archive.file.get().asFile ==
                    packageTask.outputDirectory.file('app.aot').get().asFile
                assert !archive.directory.present
                assert packageTask.packageStartupArchiveType.get() ==
                    QuarkusApplicationJvmStartupArchiveType.AOT
                assert packageTask.packageOperationForcedProperties.get() == [
                    'quarkus.package.jar.aot.enabled': 'true',
                    'quarkus.package.jar.aot.type': 'aot',
                    'quarkus.package.jar.aot.phase': 'build'
                ]
                """);
    }

    private void writeArchiveFreeAotJarWithSyntheticImageExtension() throws IOException {
        writeFile("settings.gradle", """
                rootProject.name = 'archive-free-aot-image'
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
                        aotJar('ordinary') {
                            image {
                                imageReference.set('quay.io/acme/archive-free:1.0')
                            }
                        }
                    }
                }
                """);
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

                import java.util.List;
                import java.util.Map;
                import java.util.Optional;

                import org.eclipse.microprofile.config.ConfigProvider;

                import io.quarkus.container.spi.ContainerImageInfoBuildItem;
                import io.quarkus.deployment.annotations.BuildStep;
                import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;

                class SyntheticImageProcessor {

                    @BuildStep
                    ContainerImageInfoBuildItem imageInfo() {
                        return new ContainerImageInfoBuildItem(
                                Optional.of("quay.io"), Optional.empty(), Optional.empty(),
                                "acme/archive-free", "1.0", List.of());
                    }

                    @BuildStep
                    ArtifactResultBuildItem syntheticContainerImage() {
                        String aotEnabled = ConfigProvider.getConfig()
                                .getOptionalValue("quarkus.package.jar.aot.enabled", String.class)
                                .orElseThrow(() -> new IllegalStateException(
                                        "quarkus.package.jar.aot.enabled was not forced for the ordinary image"));
                        if (!"false".equals(aotEnabled)) {
                            throw new IllegalStateException(
                                    "Ordinary AOT-JAR image must disable startup archive generation, got "
                                            + aotEnabled);
                        }
                        return new ArtifactResultBuildItem(null, "jar-container", Map.of(
                                "container-image", "quay.io/acme/archive-free:1.0",
                                "pull-required", "false",
                                "working-directory", "/synthetic-work",
                                "output-directory", "synthetic-output"));
                    }
                }
                """);
    }
}
