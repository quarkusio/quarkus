package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserCodeException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.attributes.java.TargetJvmEnvironment;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.application.QuarkusApplicationPlugin;
import io.quarkus.gradle.application.dsl.QuarkusAotJarOutput;
import io.quarkus.gradle.application.dsl.QuarkusApplicationBuild;
import io.quarkus.gradle.application.dsl.QuarkusApplicationExtension;
import io.quarkus.gradle.application.dsl.QuarkusApplicationJarOutput;
import io.quarkus.gradle.application.dsl.QuarkusApplicationRunnerOutput;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentImageSource;
import io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType;
import io.quarkus.gradle.application.model.QuarkusApplicationVariantAttributes;
import io.quarkus.gradle.application.tasks.QuarkusApplicationBuildTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationEffectiveConfigTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationPackageTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationPrepareOfflineTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationShowEffectiveConfigTask;

class QuarkusApplicationNamedBuildRegistrationTest {

    private static final Instant DEFAULT_PACKAGE_OUTPUT_TIMESTAMP = Instant.parse("1970-01-02T00:00:00Z");

    @Test
    void namedBuildsParticipateInAssembleOnlyByExplicitOptIn() {
        Project project = ProjectBuilder.builder().build();
        project.setVersion("1.2.3");
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);

        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        QuarkusApplicationBuild defaultBuild = extension.getBuilds().fastJar("default").get();
        QuarkusApplicationBuild selectedJar = extension.getBuilds().fastJar("selected").get();
        QuarkusApplicationBuild selectedNative = extension.getBuilds().nativeSources("native").get();

        assertThat(defaultBuild.getParticipatesInAssemble().get()).isFalse();
        assertThat(selectedJar.getParticipatesInAssemble().get()).isFalse();
        assertThat(selectedNative.getParticipatesInAssemble().get()).isFalse();

        Task assemble = project.getTasks().getByName(BasePlugin.ASSEMBLE_TASK_NAME);
        assertThat(assemble.getTaskDependencies().getDependencies(assemble))
                .extracting(Task::getName)
                .doesNotContain(
                        "quarkusDefaultBuild",
                        "quarkusSelectedBuild",
                        "quarkusNativeBuild");

        selectedJar.getParticipatesInAssemble().set(true);
        selectedNative.getParticipatesInAssemble().set(true);

        assertThat(assemble.getTaskDependencies().getDependencies(assemble))
                .extracting(Task::getName)
                .contains("quarkusSelectedBuild", "quarkusNativeBuild")
                .doesNotContain(
                        "quarkusDefaultBuild",
                        "quarkusSelectedImageBuild",
                        "quarkusSelectedImagePush",
                        "quarkusSelectedRun",
                        "quarkusNativeImageBuild",
                        "quarkusNativeImagePush");
    }

    @Test
    void namedBuildOfflinePreparationIsExplicitAndDependencyOnly() {
        Project project = ProjectBuilder.builder().build();
        project.setVersion("1.2.3");
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);

        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        QuarkusApplicationBuild selected = extension.getBuilds().fastJar("selected").get();
        QuarkusApplicationBuild notSelected = extension.getBuilds().nativeExecutable("notSelected").get();
        extension.getBuilds().fastJar("application").get();

        assertThat(selected.getPrepareForOffline().get()).isFalse();
        assertThat(notSelected.getPrepareForOffline().get()).isFalse();

        QuarkusApplicationPrepareOfflineTask aggregate = (QuarkusApplicationPrepareOfflineTask) project.getTasks()
                .getByName("quarkusApplicationPrepareOffline");
        assertThat(aggregate.getGroup()).isEqualTo("quarkus application");
        assertThat(aggregate.getDescription())
                .isEqualTo("Resolves standalone Quarkus application dependencies for offline use.");
        assertThat(aggregate.getTaskDependencies().getDependencies(aggregate))
                .extracting(Task::getName)
                .contains("quarkusApplicationModelPomClosure")
                .doesNotContain(
                        "quarkusApplicationSelectedPrepareOffline",
                        "quarkusApplicationNotSelectedPrepareOffline",
                        "quarkusSelectedBuild",
                        "quarkusSelectedImageBuild",
                        "quarkusSelectedImagePush");

        selected.getPrepareForOffline().set(true);

        assertThat(aggregate.getTaskDependencies().getDependencies(aggregate))
                .extracting(Task::getName)
                .contains("quarkusApplicationModelPomClosure", "quarkusApplicationSelectedPrepareOffline")
                .doesNotContain(
                        "quarkusApplicationNotSelectedPrepareOffline",
                        "quarkusSelectedBuild",
                        "quarkusSelectedImageBuild",
                        "quarkusSelectedImagePush");
        QuarkusApplicationPrepareOfflineTask selectedPreparation = (QuarkusApplicationPrepareOfflineTask) project
                .getTasks().getByName("quarkusApplicationSelectedPrepareOffline");
        assertThat(project.getTasks().getByName("quarkusApplicationApplicationPrepareOffline"))
                .isInstanceOf(QuarkusApplicationPrepareOfflineTask.class);
        assertThat(selectedPreparation.getGroup()).isNull();
        assertThat(selectedPreparation.getDescription())
                .isEqualTo("Resolves additional dependencies selected for the 'selected' Quarkus application build "
                        + "by the aggregate offline-preparation task.");
        assertThat(selectedPreparation.getPreparationScopes().get())
                .containsExactly("named build 'selected'");
        assertThat(selectedPreparation.getTaskDependencies().getDependencies(selectedPreparation))
                .extracting(Task::getName)
                .doesNotContain(
                        "quarkusSelectedBuild",
                        "quarkusSelectedImageBuild",
                        "quarkusSelectedImagePush");
    }

    @Test
    void wiresPackageOutputTimestampConventionToAllPackageOperations() {
        Project project = ProjectBuilder.builder().build();
        project.setVersion("1.2.3");
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);

        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        extension.builds(builds -> {
            builds.aotJar("fast", QuarkusApplicationJvmStartupArchiveType.AOT, aot -> {
                aot.startupArchive(archive -> archive.fromPackageBuild());
                aot.startupOptimizedImage(ignored -> {
                });
                aot.deployments(deployments -> deployments.kubernetes("dev"));
            });
            builds.nativeExecutable("native");
            builds.nativeSources("nativeSources");
        });

        String[] packageOperationTasks = {
                "quarkusApplicationRemoteDevBuild",
                "quarkusFastBuild",
                "quarkusFastRun",
                "quarkusFastImageBuild",
                "quarkusFastImagePush",
                "quarkusFastStartupOptimizedImageBuild",
                "quarkusFastStartupOptimizedImagePush",
                "quarkusFastDeployToDev",
                "quarkusNativeBuild",
                "quarkusNativeSourcesBuild"
        };
        assertPackageOutputTimestamp(project, DEFAULT_PACKAGE_OUTPUT_TIMESTAMP, packageOperationTasks);

        Instant customTimestamp = Instant.parse("2026-07-21T12:34:56Z");
        extension.getPackageOutputTimestamp().set(customTimestamp);
        assertPackageOutputTimestamp(project, customTimestamp, packageOperationTasks);

        extension.getPackageOutputTimestamp().unset();
        assertPackageOutputTimestamp(project, DEFAULT_PACKAGE_OUTPUT_TIMESTAMP, packageOperationTasks);

        extension.getPackageOutputTimestamp().unsetConvention();
        for (String taskName : packageOperationTasks) {
            QuarkusApplicationBuildTask task = (QuarkusApplicationBuildTask) project.getTasks().getByName(taskName);
            assertThat(task.getPackageOutputTimestamp().isPresent()).as(taskName).isFalse();
        }
    }

    @Test
    void exposesManifestDslOnlyForJarOutputs() {
        Project project = ProjectBuilder.builder().build();
        project.setVersion("1.2.3");
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);

        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        assertThat(extension.getBuilds().fastJar("fast").get()).isInstanceOf(QuarkusApplicationJarOutput.class);
        assertThat(extension.getBuilds().mutableJar("mutable").get()).isInstanceOf(QuarkusApplicationJarOutput.class);
        assertThat(extension.getBuilds().aotJar("aot").get()).isInstanceOf(QuarkusApplicationJarOutput.class);
        assertThat(extension.getBuilds().legacyJar("legacy").get()).isInstanceOf(QuarkusApplicationJarOutput.class);
        assertThat(extension.getBuilds().uberJar("uber").get()).isInstanceOf(QuarkusApplicationJarOutput.class);
        assertThat(extension.getBuilds().nativeExecutable("native").get())
                .isNotInstanceOf(QuarkusApplicationJarOutput.class);
        assertThat(extension.getBuilds().nativeSources("nativeSources").get())
                .isNotInstanceOf(QuarkusApplicationJarOutput.class);

        assertThatThrownBy(() -> QuarkusApplicationBuild.class.getMethod("getManifestAttributes"))
                .isInstanceOf(NoSuchMethodException.class);
        assertThatThrownBy(() -> QuarkusApplicationPackageTask.class.getMethod("getManifestAttributes"))
                .isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void registersCompletePackageAndProducerLocalLauncherVariantsForEveryJvmBuild() {
        Project project = ProjectBuilder.builder().withName("variants").build();
        project.setVersion("1.2.3");
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);

        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        extension.builds(builds -> {
            builds.fastJar("fast");
            builds.aotJar("aot");
            builds.mutableJar("mutable");
            builds.legacyJar("legacy");
            builds.uberJar("uber");
            builds.nativeExecutable("native");
            builds.nativeSources("nativeSources");
        });

        assertPackageVariants(project, "Fast", "fast", "fast-jar", "quarkus-run.jar");
        assertPackageVariants(project, "Aot", "aot", "aot-jar", "quarkus-run.jar");
        assertPackageVariants(project, "Mutable", "mutable", "mutable-jar", "quarkus-run.jar");
        assertPackageVariants(project, "Legacy", "legacy", "legacy-jar", "variants-1.2.3-runner.jar");
        assertPackageVariants(project, "Uber", "uber", "uber-jar", "variants-1.2.3-runner.jar");
        assertThat(project.getConfigurations().findByName("quarkusNativePackageElements")).isNull();
        assertThat(project.getConfigurations().findByName("quarkusNativeLauncherJarElements")).isNull();
        assertThat(project.getConfigurations().findByName("quarkusNativeSourcesPackageElements")).isNull();
        assertThat(project.getConfigurations().findByName("quarkusNativeSourcesLauncherJarElements")).isNull();
    }

    @Test
    void wiresIsolatedManifestPropertiesToEveryJarBuildOperation() {
        Project project = ProjectBuilder.builder().build();
        project.setVersion("1.2.3");
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);

        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        QuarkusAotJarOutput app = extension.getBuilds().aotJar("app", build -> {
            build.getManifest().getAttributes().put("Built-By", "app");
            build.getManifest().sections(sections -> sections.section("Specification",
                    section -> section.getAttributes().put("Specification-Title", "Application")));
            build.startupOptimizedImage(ignored -> {
            });
            build.deployments(deployments -> deployments.kubernetes("dev"));
        }).get();
        extension.getBuilds().fastJar("other",
                build -> build.getManifest().getAttributes().put("Built-By", "other"));
        extension.getBuilds().nativeExecutable("native");
        extension.getBuilds().nativeSources("nativeSources");

        project.getTasks().getByName("quarkusAppBuild");
        app.getManifest().getSections().section("Late",
                section -> section.getAttributes().put("Late-Attribute", "late"));

        Map<String, String> expected = Map.of(
                "quarkus.package.jar.manifest.attributes.\"Built-By\"", "app",
                "quarkus.package.jar.manifest.sections.\"Specification\".\"Specification-Title\"", "Application",
                "quarkus.package.jar.manifest.sections.\"Late\".\"Late-Attribute\"", "late");
        String[] appOperations = {
                "quarkusAppBuild",
                "quarkusAppShowEffectiveConfig",
                "quarkusAppRun",
                "quarkusAppImageBuild",
                "quarkusAppImagePush",
                "quarkusAppStartupOptimizedImageBuild",
                "quarkusAppStartupOptimizedImagePush",
                "quarkusAppDeployToDev"
        };
        for (String taskName : appOperations) {
            assertThat(manifestProperties(project, taskName)).as(taskName).containsExactlyInAnyOrderEntriesOf(expected);
        }

        assertThat(manifestProperties(project, "quarkusOtherBuild"))
                .containsExactlyEntriesOf(
                        Map.of("quarkus.package.jar.manifest.attributes.\"Built-By\"", "other"));
        assertThat(manifestProperties(project, "quarkusNativeBuild")).isEmpty();
        assertThat(manifestProperties(project, "quarkusNativeSourcesBuild")).isEmpty();
        assertThat(manifestProperties(project, "quarkusApplicationRemoteDevBuild")).isEmpty();
    }

    @Test
    void assignsHelpfulDescriptionsToRegisteredTasks() {
        Project project = ProjectBuilder.builder().build();
        project.setVersion("1.2.3");
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);

        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        extension.builds(builds -> {
            builds.aotJar("fast", QuarkusApplicationJvmStartupArchiveType.AOT, build -> {
                build.image(image -> image.getRepository().set("example/fast"));
                build.startupArchive(archive -> archive.getFile().set(
                        project.getLayout().getProjectDirectory().file("build/aot/fast.aot")));
                build.startupOptimizedImage(ignored -> {
                });
                build.deployments(deployments -> {
                    deployments.kubernetes("dev");
                    deployments.openshift("prod", deployment -> deployment.getImageSource()
                            .set(QuarkusApplicationDeploymentImageSource.STARTUP_OPTIMIZED_IMAGE_PUSH));
                });
            });
            builds.legacyJar("legacy");
            builds.mutableJar("mutable");
            builds.uberJar("uber");
            builds.nativeExecutable("native");
            builds.nativeSources("nativeSources");
        });

        assertTaskDescription(project, "quarkusApplicationModel",
                "Resolves the Quarkus application model used by named application build tasks.");
        assertTaskDescription(project, "quarkusApplicationCodegenModel",
                "Resolves the Quarkus application model used before main-source code generation.");
        assertTaskDescription(project, "quarkusApplicationDevCodegenModel",
                "Resolves the Quarkus application model used before development-mode code generation.");
        assertTaskDescription(project, "quarkusApplicationTestCodegenModel",
                "Resolves the Quarkus application model used before test-source code generation.");
        assertTaskDescription(project, "quarkusApplicationContinuousTestModel",
                "Resolves the Quarkus application model used by Gradle-native continuous testing.");
        assertTaskDescription(project, "quarkusApplicationShowModel",
                "Displays the generated normal Quarkus application model.");
        assertTaskDescription(project, "quarkusApplicationShowDevModel",
                "Displays the generated development Quarkus application model.");
        assertTaskDescription(project, "quarkusApplicationShowTestModel",
                "Displays the generated test Quarkus application model.");
        assertTaskDescription(project, "quarkusApplicationGenerateCode",
                "Runs Quarkus code generators for main sources.");
        assertTaskDescription(project, "quarkusApplicationGenerateDevCode",
                "Runs Quarkus code generators for main sources in development mode.");
        assertTaskDescription(project, "quarkusApplicationGenerateTestCode",
                "Runs Quarkus code generators for test sources.");
        assertTaskDescription(project, "quarkusApplicationDev",
                "Runs Gradle-native Quarkus dev mode using Gradle continuous build.");
        assertTaskDescription(project, "quarkusApplicationRemoteDevBuild",
                "Builds the internal mutable-jar package used by Gradle-native Quarkus remote dev.");
        assertTaskDescription(project, "quarkusApplicationRemoteDev",
                "Runs Gradle-native Quarkus remote dev using an internal mutable-jar package.");

        assertTaskDescription(project, "quarkusFastBuild", "Builds the 'fast' aot-jar Quarkus application.");
        assertTaskDescription(project, "quarkusFastShowEffectiveConfig",
                "Shows effective Quarkus configuration for the 'fast' application build.");
        assertTaskDescription(project, "quarkusLegacyBuild", "Builds the 'legacy' legacy-jar Quarkus application.");
        assertTaskDescription(project, "quarkusMutableBuild", "Builds the 'mutable' mutable-jar Quarkus application.");
        assertTaskDescription(project, "quarkusUberBuild", "Builds the 'uber' uber-jar Quarkus application.");
        assertTaskDescription(project, "quarkusNativeBuild",
                "Builds the 'native' native executable Quarkus application.");
        assertTaskDescription(project, "quarkusNativeSourcesBuild",
                "Generates native-image sources for the 'nativeSources' Quarkus application.");

        assertTaskDescription(project, "quarkusNativeNativeTest",
                "Runs tests against the 'native' native executable.");
        assertTaskDescription(project, "quarkusFastImageBuild",
                "Builds the container image for the 'fast' Quarkus application build.");
        assertTaskDescription(project, "quarkusFastImagePush",
                "Builds and pushes the container image for the 'fast' Quarkus application build.");
        assertTaskDescription(project, "quarkusFastStartupOptimizedImageBuild",
                "Builds the startup-optimized container image for the 'fast' Quarkus application build.");
        assertTaskDescription(project, "quarkusFastStartupOptimizedImagePush",
                "Builds and pushes the startup-optimized container image for the 'fast' Quarkus application build.");
        assertTaskDescription(project, "quarkusFastDeployToDev",
                "Deploys the 'fast' Quarkus application build to the 'dev' kubernetes target.");
        assertTaskDescription(project, "quarkusFastDeployToProd",
                "Deploys the 'fast' Quarkus application build to the 'prod' openshift target.");
        assertTaskDescription(project, "quarkusFastRun",
                "Runs the 'fast' Quarkus application from its package build output.");
        assertThat(project.getTasks().findByName("quarkusFastContinuousTest")).isNull();
        assertThat(project.getTasks().findByName("quarkusFastDev")).isNull();
        assertThat(project.getTasks().findByName("quarkusFastRemoteDev")).isNull();
        assertThat(project.getTasks().findByName("quarkusMutableRemoteDev")).isNull();
        assertThat(project.getTasks().findByName("quarkusLegacyRemoteDev")).isNull();
        assertThat(project.getTasks().findByName("quarkusUberRemoteDev")).isNull();
        assertThat(project.getTasks().findByName("quarkusNativeRun")).isNull();
        assertThat(project.getTasks().findByName("quarkusNativeSourcesRun")).isNull();
        assertThat(project.getTasks().findByName("quarkusNativeRemoteDev")).isNull();
        assertThat(project.getTasks().findByName("quarkusNativeSourcesRemoteDev")).isNull();

        assertTaskGroup(project, "quarkusFastBuild", "quarkus application");
        assertTaskGroup(project, "quarkusFastShowEffectiveConfig", "quarkus application");
        assertTaskGroup(project, "quarkusFastRun", "quarkus application");
        assertTaskGroup(project, "quarkusApplicationRemoteDevBuild", "quarkus application");
        assertTaskGroup(project, "quarkusApplicationRemoteDev", "quarkus application");
        assertTaskGroup(project, "quarkusFastImageBuild", "quarkus application");
        assertTaskGroup(project, "quarkusFastDeployToDev", "quarkus application");
        assertTaskGroup(project, "quarkusApplicationDev", "quarkus application");
        assertTaskGroup(project, "quarkusNativeNativeTest", "verification");
    }

    @Test
    void wiresArchiveNamingAndRunnerSuffixConventionsByOutputShape() {
        Project project = ProjectBuilder.builder().withName("archive-app").build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        project.setVersion("1.2.3");

        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        extension.builds(builds -> {
            builds.fastJar("fast", fast -> assertThat(fast.getOutputName().get()).isEqualTo("archive-app-1.2.3"));
            builds.mutableJar("mutable");
            builds.legacyJar("legacy");
            builds.uberJar("uber", uber -> {
                uber.getArchiveBaseNameSuffix().set("-cli");
                uber.getArchiveRunnerSuffix().set("-exec");
                uber.getArchiveAddRunnerSuffix().set(false);
            });
            builds.nativeExecutable("native",
                    nativeBuild -> nativeBuild.getNativeArguments().put("quarkus.native.additional-build-args", "-O2"));
            builds.nativeSources("nativeSources");
        });

        Map<String, QuarkusApplicationBuild> buildsByName = new LinkedHashMap<>();
        extension.getBuilds().all(build -> buildsByName.put(build.getName(), build));

        assertThat(buildsByName.get("fast"))
                .isNotInstanceOf(QuarkusApplicationRunnerOutput.class);
        assertThat(buildsByName.get("mutable"))
                .isNotInstanceOf(QuarkusApplicationRunnerOutput.class);
        assertThat(buildsByName.get("legacy"))
                .isInstanceOf(QuarkusApplicationRunnerOutput.class);
        assertThat(buildsByName.get("uber"))
                .isInstanceOf(QuarkusApplicationRunnerOutput.class);
        assertThat(buildsByName.get("native"))
                .isInstanceOf(QuarkusApplicationRunnerOutput.class);
        assertThat(buildsByName.get("nativeSources"))
                .isInstanceOf(QuarkusApplicationRunnerOutput.class);

        QuarkusApplicationPackageTask fast = (QuarkusApplicationPackageTask) project.getTasks()
                .getByName("quarkusFastBuild");
        QuarkusApplicationPackageTask mutable = (QuarkusApplicationPackageTask) project.getTasks()
                .getByName("quarkusMutableBuild");
        QuarkusApplicationPackageTask legacy = (QuarkusApplicationPackageTask) project.getTasks()
                .getByName("quarkusLegacyBuild");
        QuarkusApplicationPackageTask uber = (QuarkusApplicationPackageTask) project.getTasks()
                .getByName("quarkusUberBuild");
        QuarkusApplicationBuildTask nativeExecutable = (QuarkusApplicationBuildTask) project.getTasks()
                .getByName("quarkusNativeBuild");
        QuarkusApplicationBuildTask nativeSources = (QuarkusApplicationBuildTask) project.getTasks()
                .getByName("quarkusNativeSourcesBuild");
        QuarkusApplicationShowEffectiveConfigTask uberEffectiveConfig = (QuarkusApplicationShowEffectiveConfigTask) project
                .getTasks().getByName("quarkusUberShowEffectiveConfig");
        QuarkusApplicationShowEffectiveConfigTask nativeEffectiveConfig = (QuarkusApplicationShowEffectiveConfigTask) project
                .getTasks().getByName("quarkusNativeShowEffectiveConfig");

        assertThat(fast.getOutputName().get()).isEqualTo("archive-app-1.2.3");
        assertThat(mutable.getOutputName().get()).isEqualTo("archive-app-1.2.3");
        assertThat(legacy.getOutputName().get()).isEqualTo("archive-app-1.2.3");
        assertThat(uber.getOutputName().get()).isEqualTo("archive-app-cli-1.2.3");
        assertThat(nativeExecutable.getOutputName().get()).isEqualTo("archive-app-1.2.3");
        assertThat(nativeSources.getOutputName().get()).isEqualTo("archive-app-1.2.3");

        assertThat(fast.getAdditionalDescriptorShapeProperties().get()).isEmpty();
        assertThat(mutable.getAdditionalDescriptorShapeProperties().get()).isEmpty();
        assertThat(legacy.getAdditionalDescriptorShapeProperties().get()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "quarkus.package.runner-suffix", "-runner",
                "quarkus.package.jar.add-runner-suffix", "true"));
        assertThat(uber.getAdditionalDescriptorShapeProperties().get()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "quarkus.package.runner-suffix", "-exec",
                "quarkus.package.jar.add-runner-suffix", "false"));
        assertThat(nativeExecutable.getAdditionalDescriptorShapeProperties().get()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "quarkus.package.runner-suffix", "-runner",
                "quarkus.package.jar.add-runner-suffix", "true"));
        assertThat(nativeSources.getAdditionalDescriptorShapeProperties().get()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "quarkus.package.runner-suffix", "-runner",
                "quarkus.package.jar.add-runner-suffix", "true"));
        assertThat(uberEffectiveConfig.getAdditionalDescriptorShapeProperties().get())
                .containsExactlyInAnyOrderEntriesOf(uber.getAdditionalDescriptorShapeProperties().get());
        assertThat(uberEffectiveConfig.getOutputName().get()).isEqualTo(uber.getOutputName().get());
        assertThat(uberEffectiveConfig.getOutputDirectory().get().getAsFile())
                .isEqualTo(uber.getOutputDirectory().get().getAsFile());
        assertThat(nativeEffectiveConfig.getBuildOperationForcedProperties().get())
                .containsEntry("quarkus.native.additional-build-args", "-O2");
    }

    @Test
    void primaryJarFileIsKnownBeforePackageTaskExecutes() {
        Project project = ProjectBuilder.builder().withName("archive-app").build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        project.setVersion("1.2.3");

        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        extension.builds(builds -> {
            builds.fastJar("fast");
            builds.mutableJar("mutable");
            builds.legacyJar("legacy");
            builds.uberJar("uber", uber -> {
                uber.getArchiveBaseNameSuffix().set("-cli");
                uber.getArchiveRunnerSuffix().set("-exec");
                uber.getArchiveAddRunnerSuffix().set(false);
            });
        });

        Path buildDirectory = project.getLayout().getBuildDirectory().get().getAsFile().toPath();
        assertPrimaryJarFile(project, "quarkusFastBuild",
                buildDirectory.resolve(Path.of("quarkus-builds", "fast", "package", "quarkus-run.jar")));
        assertPrimaryJarFile(project, "quarkusMutableBuild",
                buildDirectory.resolve(Path.of("quarkus-builds", "mutable", "package", "quarkus-run.jar")));
        assertPrimaryJarFile(project, "quarkusLegacyBuild",
                buildDirectory.resolve(Path.of("quarkus-builds", "legacy", "package", "archive-app-1.2.3-runner.jar")));
        assertPrimaryJarFile(project, "quarkusUberBuild",
                buildDirectory.resolve(Path.of("quarkus-builds", "uber", "package", "archive-app-cli-1.2.3.jar")));
    }

    @Test
    void rejectsNamedTaskCollisionsAtRegistrationTime() {
        Project project = ProjectBuilder.builder().build();
        project.getTasks().register("quarkusAppBuild");
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);

        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);

        assertThatThrownBy(() -> extension.builds(builds -> builds.fastJar("app")))
                .isInstanceOf(InvalidUserCodeException.class)
                .hasRootCauseInstanceOf(GradleException.class)
                .hasRootCauseMessage("Quarkus application task name 'quarkusAppBuild' collides with an existing task");
    }

    @Test
    void rejectsUnspecifiedProjectVersionOnlyWhenDefaultOutputNameConventionIsUsed() {
        Project project = ProjectBuilder.builder().withName("unnamed-app").build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);

        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        extension.builds(builds -> {
            builds.fastJar("defaultName");
            builds.fastJar("explicitName", build -> build.getOutputName().set("explicit-unspecified"));
        });

        QuarkusApplicationPackageTask defaultName = (QuarkusApplicationPackageTask) project.getTasks()
                .getByName("quarkusDefaultNameBuild");
        QuarkusApplicationPackageTask explicitName = (QuarkusApplicationPackageTask) project.getTasks()
                .getByName("quarkusExplicitNameBuild");

        assertThatThrownBy(() -> defaultName.getOutputName().get())
                .hasRootCauseInstanceOf(GradleException.class)
                .hasRootCauseMessage("Quarkus application archiveVersion defaults to project.version, "
                        + "but project.version is unspecified. Configure project.version, archiveVersion, or outputName.");
        assertThat(explicitName.getOutputName().get()).isEqualTo("explicit-unspecified");
    }

    private static void assertTaskDescription(Project project, String taskName, String description) {
        assertThat(project.getTasks().getByName(taskName).getDescription()).isEqualTo(description);
    }

    private static void assertTaskGroup(Project project, String taskName, String group) {
        assertThat(project.getTasks().getByName(taskName).getGroup()).isEqualTo(group);
    }

    private static void assertPrimaryJarFile(Project project, String taskName, Path expected) {
        QuarkusApplicationPackageTask task = (QuarkusApplicationPackageTask) project.getTasks().getByName(taskName);
        assertThat(task.getPrimaryJarFile().get().toPath()).isEqualTo(expected);
    }

    private static void assertPackageVariants(Project project, String taskSegment, String buildName, String buildType,
            String launcherName) {
        String buildTaskName = "quarkus" + taskSegment + "Build";
        QuarkusApplicationPackageTask buildTask = (QuarkusApplicationPackageTask) project.getTasks()
                .getByName(buildTaskName);
        Path outputDirectory = buildTask.getOutputDirectory().get().getAsFile().toPath();
        assertOutgoingVariant(project.getConfigurations().getByName("quarkus" + taskSegment + "PackageElements"),
                buildName, buildType, QuarkusApplicationVariantAttributes.PACKAGE_CATEGORY,
                QuarkusApplicationVariantAttributes.PACKAGE_LIBRARY_ELEMENTS,
                ArtifactTypeDefinition.DIRECTORY_TYPE, outputDirectory, buildTaskName);
        assertOutgoingVariant(project.getConfigurations().getByName("quarkus" + taskSegment + "LauncherJarElements"),
                buildName, buildType, QuarkusApplicationVariantAttributes.LAUNCHER_CATEGORY,
                QuarkusApplicationVariantAttributes.LAUNCHER_LIBRARY_ELEMENTS,
                ArtifactTypeDefinition.JAR_TYPE, outputDirectory.resolve(launcherName), buildTaskName);
    }

    private static void assertOutgoingVariant(Configuration configuration, String buildName, String buildType,
            String category, String libraryElements, String artifactType, Path artifactPath, String buildTaskName) {
        assertThat(configuration.isCanBeConsumed()).isTrue();
        assertThat(configuration.isCanBeResolved()).isFalse();
        assertThat(configuration.isCanBeDeclared()).isFalse();
        assertThat(configuration.getAttributes().getAttribute(Category.CATEGORY_ATTRIBUTE).getName()).isEqualTo(category);
        assertThat(configuration.getAttributes().getAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE).getName())
                .isEqualTo(libraryElements);
        assertThat(configuration.getAttributes().getAttribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE))
                .isEqualTo(artifactType);
        assertThat(configuration.getAttributes().getAttribute(QuarkusApplicationVariantAttributes.BUILD_NAME_ATTRIBUTE))
                .isEqualTo(buildName);
        assertThat(configuration.getAttributes().getAttribute(QuarkusApplicationVariantAttributes.BUILD_TYPE_ATTRIBUTE))
                .isEqualTo(buildType);
        assertThat(configuration.getAttributes().getAttribute(Usage.USAGE_ATTRIBUTE)).isNull();
        assertThat(configuration.getAttributes().getAttribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE))
                .isNull();
        assertThat(configuration.getAttributes().getAttribute(Bundling.BUNDLING_ATTRIBUTE)).isNull();
        assertThat(configuration.getOutgoing().getArtifacts()).singleElement().satisfies(artifact -> {
            assertThat(artifact.getType()).isEqualTo(artifactType);
            assertThat(artifact.getFile().toPath()).isEqualTo(artifactPath);
            assertThat(artifact.getBuildDependencies().getDependencies(null))
                    .extracting(Task::getName)
                    .containsExactly(buildTaskName);
        });
    }

    private static void assertPackageOutputTimestamp(Project project, Instant expected, String... taskNames) {
        for (String taskName : taskNames) {
            QuarkusApplicationBuildTask task = (QuarkusApplicationBuildTask) project.getTasks().getByName(taskName);
            assertThat(task.getPackageOutputTimestamp().get()).as(taskName).isEqualTo(expected);
        }
    }

    private static Map<String, String> manifestProperties(Project project, String taskName) {
        QuarkusApplicationEffectiveConfigTask task = (QuarkusApplicationEffectiveConfigTask) project.getTasks()
                .getByName(taskName);
        return task.getManifestConfigProperties().get();
    }
}
