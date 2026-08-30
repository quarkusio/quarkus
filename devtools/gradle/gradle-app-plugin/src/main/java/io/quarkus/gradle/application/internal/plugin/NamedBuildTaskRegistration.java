package io.quarkus.gradle.application.internal.plugin;

import static io.quarkus.gradle.application.internal.plugin.TaskRegistrationSupport.QUARKUS_APPLICATION_GROUP;
import static io.quarkus.gradle.application.internal.plugin.TaskRegistrationSupport.configureConfigInputs;
import static io.quarkus.gradle.application.internal.plugin.TaskRegistrationSupport.configureForkOptions;
import static io.quarkus.gradle.application.internal.plugin.TaskRegistrationSupport.configureJavaClasspath;
import static io.quarkus.gradle.application.internal.plugin.TaskRegistrationSupport.configureJavaSourceDirectories;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import io.quarkus.gradle.application.dsl.QuarkusAotJarOutput;
import io.quarkus.gradle.application.dsl.QuarkusApplicationBuild;
import io.quarkus.gradle.application.dsl.QuarkusApplicationConfigInputs;
import io.quarkus.gradle.application.dsl.QuarkusApplicationExtension;
import io.quarkus.gradle.application.dsl.QuarkusApplicationJarOutput;
import io.quarkus.gradle.application.dsl.QuarkusApplicationRunnerOutput;
import io.quarkus.gradle.application.internal.config.ManifestConfigProperties;
import io.quarkus.gradle.application.internal.image.ImageReferenceClaimService;
import io.quarkus.gradle.application.internal.modelgen.GenerateModelTask;
import io.quarkus.gradle.application.internal.planning.TaskNamePlanner;
import io.quarkus.gradle.application.internal.planning.TaskNameSegment;
import io.quarkus.gradle.application.internal.planning.TaskNames;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentImageSource;
import io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType;
import io.quarkus.gradle.application.tasks.QuarkusApplicationBuildTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationDeployTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationEffectiveConfigTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationImageBuildTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationIntegrationTestMetadataTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationNativeTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationPackageTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationRunTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationShowEffectiveConfigTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationTask;

final class NamedBuildTaskRegistration {

    private final TaskNamePlanner planner = new TaskNamePlanner();
    private final Map<String, BuildRegistration> buildNames = new HashMap<>();
    private final TaskNameRegistry taskNames;
    private final TaskProvider<GenerateModelTask> applicationModel;
    private final QuarkusApplicationIntegrationTestConfigurator integrationTestConfigurator;
    private final OfflinePreparationRegistration offlinePreparation;
    private final Provider<ImageReferenceClaimService> imageReferenceClaims;
    private final DslLifecycleCoordinator lifecycle;

    NamedBuildTaskRegistration(TaskNameRegistry taskNames,
            TaskProvider<GenerateModelTask> applicationModel,
            QuarkusApplicationIntegrationTestConfigurator integrationTestConfigurator,
            OfflinePreparationRegistration offlinePreparation,
            Provider<ImageReferenceClaimService> imageReferenceClaims,
            DslLifecycleCoordinator lifecycle) {
        this.taskNames = taskNames;
        this.applicationModel = applicationModel;
        this.integrationTestConfigurator = integrationTestConfigurator;
        this.offlinePreparation = offlinePreparation;
        this.imageReferenceClaims = imageReferenceClaims;
        this.lifecycle = lifecycle;
    }

    void register(Project project, QuarkusApplicationExtension extension, QuarkusApplicationBuild build) {
        BuildRegistration buildRegistration = validateNamedBuild(build);
        TaskNames names = planner.taskNames(buildRegistration.descriptor());
        offlinePreparation.registerNamedBuild(build);

        TaskProvider<? extends QuarkusApplicationBuildTask> namedBuild = registerNamedBuildTask(project, extension,
                buildRegistration,
                names.build());
        registerAssembleLifecycleDependency(project, build, namedBuild);
        registerNamedShowEffectiveConfigTask(project, extension, buildRegistration, names.showEffectiveConfig());
        if (buildRegistration.type().isJar()) {
            new NamedPackageVariantRegistration().register(project, buildRegistration.descriptor(), namedBuild);
        }
        if (buildRegistration.type().isJar()) {
            registerNamedRunTask(project, extension, buildRegistration, names.run(), names.build());
        }
        NamedImageTaskRegistration imageRegistration = new NamedImageTaskRegistration(taskNames, applicationModel,
                imageReferenceClaims);
        NamedImageTaskRegistration.NamedImageTasks imageTasks = imageRegistration.registerNormalImages(project, extension,
                buildRegistration, names);
        QuarkusApplicationIntegrationTestBuild integrationTestBuild = registerIntegrationTestBuild(
                project, buildRegistration, namedBuild, imageTasks.imageBuild(), names);
        if (buildRegistration.type() == QuarkusApplicationBuildType.NATIVE_EXECUTABLE) {
            integrationTestConfigurator.registerNativeTestSuite(integrationTestBuild, names.nativeTest());
        }
        if (build instanceof QuarkusAotJarOutput aotJar) {
            configurePackageStartupArchiveSource(namedBuild, aotJar);
            // Startup-optimized image tasks depend on optional nested DSL state.
            // Register them only after that state has reached its final shape.
            lifecycle.whenStartupOptimizedImageConfigured(aotJar,
                    ignored -> imageRegistration.registerStartupOptimizedImages(project, extension, buildRegistration,
                            names, aotJar, imageTasks));
        }

        NamedDeploymentTaskRegistration deploymentRegistration = new NamedDeploymentTaskRegistration(taskNames,
                applicationModel);
        build.getDeployments().all(deployment -> {
            deploymentRegistration.validateName(deployment);
            String deployTaskName = planner.deployTaskName(buildRegistration.descriptor(),
                    new QuarkusApplicationDeploymentDescriptor(
                            deployment.getName(),
                            deployment.getTarget(),
                            deployment.getImageSource()
                                    .getOrElse(QuarkusApplicationDeploymentImageSource.NORMAL_IMAGE_PUSH),
                            Optional.ofNullable(deployment.getImageReference().getOrNull())));
            TaskProvider<QuarkusApplicationDeployTask> deploy = deploymentRegistration.register(project, extension,
                    buildRegistration, deployment, deployTaskName, imageTasks);
            if (build instanceof QuarkusAotJarOutput aotJar) {
                // The selected image source can refer to tasks that are registered
                // only after the startup-optimized image DSL has been finalized.
                lifecycle.whenStartupOptimizedImageConfigured(aotJar,
                        ignored -> deploymentRegistration.configureStartupOptimizedPredecessor(project, buildRegistration,
                                deployment, deploy, names));
            }
        });
    }

    private static void registerAssembleLifecycleDependency(Project project, QuarkusApplicationBuild build,
            TaskProvider<? extends QuarkusApplicationBuildTask> namedBuild) {
        // dependsOn accepts a provider of task references, so each named build can
        // opt in lazily without realizing the build task or joining assemble by default.
        Provider<List<TaskProvider<? extends QuarkusApplicationBuildTask>>> selectedBuild = build
                .getParticipatesInAssemble()
                .map(enabled -> enabled ? List.of(namedBuild) : List.of());
        project.getTasks().named(BasePlugin.ASSEMBLE_TASK_NAME)
                .configure(assemble -> assemble.dependsOn(selectedBuild));
    }

    private void configurePackageStartupArchiveSource(
            TaskProvider<? extends QuarkusApplicationBuildTask> namedBuild, QuarkusAotJarOutput aotJar) {
        var archive = aotJar.getStartupArchive();
        // Source selection is mutually exclusive, but users may configure it after
        // creating the named build. Validate and freeze it only at DSL finalization.
        lifecycle.whenPackageBuildConfigured(archive, ignored -> {
            if (!archive.getType().isPresent()) {
                throw new GradleException("Quarkus AOT-JAR output '" + aotJar.getName()
                        + "' must select a concrete startup archive type before fromPackageBuild()");
            }
            QuarkusApplicationJvmStartupArchiveType type = archive.getType().get();
            if (archive.getFile().isPresent() || archive.getDirectory().isPresent()) {
                throw new GradleException("Quarkus AOT-JAR output '" + aotJar.getName()
                        + "' cannot combine fromPackageBuild() with a user-supplied startup archive location");
            }
            if (type.isDirectory()) {
                archive.getDirectory()
                        .set(namedBuild.flatMap(task -> task.getOutputDirectory().dir(type.getDefaultName())));
                archive.getDirectory().disallowChanges();
                archive.getFile().disallowChanges();
            } else {
                archive.getFile()
                        .set(namedBuild.flatMap(task -> task.getOutputDirectory().file(type.getDefaultName())));
                archive.getFile().disallowChanges();
                archive.getDirectory().disallowChanges();
            }
            namedBuild.configure(task -> {
                QuarkusApplicationPackageTask packageTask = (QuarkusApplicationPackageTask) task;
                packageTask.getPackageStartupArchiveType().set(type);
                packageTask.getPackageOperationForcedProperties().put("quarkus.package.jar.aot.enabled", "true");
                packageTask.getPackageOperationForcedProperties().put("quarkus.package.jar.aot.type",
                        type.getQuarkusType());
                packageTask.getPackageOperationForcedProperties().put("quarkus.package.jar.aot.phase", "build");
            });
        });
    }

    static String packageElementsConfigurationName(String buildName) {
        return NamedPackageVariantRegistration.packageElementsConfigurationName(buildName);
    }

    static String launcherJarElementsConfigurationName(String buildName) {
        return NamedPackageVariantRegistration.launcherJarElementsConfigurationName(buildName);
    }

    private TaskProvider<? extends QuarkusApplicationBuildTask> registerNamedBuildTask(Project project,
            QuarkusApplicationExtension extension, BuildRegistration buildRegistration, String taskName) {
        taskNames.register(project, taskName);
        Class<? extends QuarkusApplicationBuildTask> taskType = buildRegistration.type().isNativeOutput()
                ? QuarkusApplicationNativeTask.class
                : QuarkusApplicationPackageTask.class;
        return project.getTasks().register(taskName, taskType, task -> {
            configureNamedBuildTask(project, extension, task, buildRegistration);
            task.setGroup(QUARKUS_APPLICATION_GROUP);
            task.setDescription(buildDescription(buildRegistration));
            if (task instanceof QuarkusApplicationPackageTask packageTask) {
                packageTask.getPackageOperationForcedProperties().convention(Map.of());
                packageTask.getPackageResultFile().set(project.getLayout().getBuildDirectory()
                        .file(packageResultPath(buildRegistration, "package-result.properties")));
            }
            if (task instanceof QuarkusApplicationNativeTask nativeTask) {
                nativeTask.getNativeArguments().set(buildRegistration.build().getNativeArguments());
                nativeTask.getNativeResultFile().set(project.getLayout().getBuildDirectory()
                        .file(packageResultPath(buildRegistration, "native-result.properties")));
            }
        });
    }

    private void registerNamedShowEffectiveConfigTask(Project project, QuarkusApplicationExtension extension,
            BuildRegistration buildRegistration, String taskName) {
        taskNames.register(project, taskName);
        project.getTasks().register(taskName, QuarkusApplicationShowEffectiveConfigTask.class, task -> {
            configureNamedEffectiveConfigTask(project, extension, task, buildRegistration);
            task.getBuildOperationForcedProperties().set(buildRegistration.build().getNativeArguments());
            task.setGroup(QUARKUS_APPLICATION_GROUP);
            task.setDescription("Shows effective Quarkus configuration for the '" + buildRegistration.name()
                    + "' application build.");
        });
    }

    private QuarkusApplicationIntegrationTestBuild registerIntegrationTestBuild(Project project,
            BuildRegistration buildRegistration,
            TaskProvider<? extends QuarkusApplicationBuildTask> namedBuild,
            TaskProvider<QuarkusApplicationImageBuildTask> imageBuild, TaskNames names) {
        Optional<TaskProvider<QuarkusApplicationIntegrationTestMetadataTask>> launcherMetadataTask = Optional.empty();
        Provider<Directory> launcherMetadataDirectory = project.getLayout().getBuildDirectory()
                .dir(packageResultDirectoryPath(buildRegistration));
        if (buildRegistration.type().isJar()) {
            String taskName = "quarkus" + TaskNameSegment.of(buildRegistration.name()).value()
                    + "IntegrationTestMetadata";
            taskNames.register(project, taskName);
            launcherMetadataDirectory = project.getLayout().getBuildDirectory()
                    .dir("quarkus-build-results/" + buildRegistration.name() + "/integration-test");
            Provider<Directory> finalLauncherMetadataDirectory = launcherMetadataDirectory;
            launcherMetadataTask = Optional.of(project.getTasks().register(taskName,
                    QuarkusApplicationIntegrationTestMetadataTask.class, task -> {
                        task.setGroup(QUARKUS_APPLICATION_GROUP);
                        task.setDescription("Generates integration-test launch metadata for the '"
                                + buildRegistration.name() + "' Quarkus application package build.");
                        task.dependsOn(namedBuild);
                        task.getPackageResultFile().set(project.getLayout().getBuildDirectory()
                                .file(packageResultPath(buildRegistration, "package-result.properties")));
                        task.getRelocatedArtifactPropertiesFile().set(project.getLayout().getBuildDirectory()
                                .file(packageResultPath(buildRegistration, "quarkus-artifact.properties")));
                        task.getLauncherMetadataDirectory().set(finalLauncherMetadataDirectory);
                    }));
        }
        var integrationTestBuild = new QuarkusApplicationIntegrationTestBuild(
                buildRegistration.name(),
                buildRegistration.type(),
                namedBuild,
                project.getLayout().getBuildDirectory().dir(packageResultDirectoryPath(buildRegistration)),
                project.getLayout().getBuildDirectory().file(packageResultPath(buildRegistration,
                        buildRegistration.type().isNativeOutput() ? "native-result.properties"
                                : "package-result.properties")),
                launcherMetadataDirectory,
                launcherMetadataTask,
                names,
                buildRegistration.build() instanceof QuarkusAotJarOutput aotJar
                        ? Optional.of(aotJar)
                        : Optional.empty(),
                buildRegistration.build() instanceof QuarkusAotJarOutput
                        ? Optional.of(imageBuild)
                        : Optional.empty(),
                buildRegistration.build() instanceof QuarkusAotJarOutput
                        ? Optional.of(project.getLayout().getBuildDirectory()
                                .file(operationResultPath(buildRegistration, "image-build",
                                        "image-build-result.properties")))
                        : Optional.empty());
        integrationTestConfigurator.registerBuild(integrationTestBuild);
        return integrationTestBuild;
    }

    private void registerNamedRunTask(Project project, QuarkusApplicationExtension extension,
            BuildRegistration buildRegistration, String taskName, String packageTaskName) {
        taskNames.register(project, taskName);
        project.getTasks().register(taskName, QuarkusApplicationRunTask.class, task -> {
            configureNamedBuildTask(project, extension, task, buildRegistration);
            task.dependsOn(packageTaskName);
            task.getPackageResultFile().set(project.getLayout().getBuildDirectory()
                    .file(packageResultPath(buildRegistration, "package-result.properties")));
            task.setGroup(QUARKUS_APPLICATION_GROUP);
            task.setDescription("Runs the '" + buildRegistration.name()
                    + "' Quarkus application from its package build output.");
        });
    }

    static String operationOutputPath(BuildRegistration buildRegistration, String operation) {
        return "quarkus-builds/" + buildRegistration.name() + "/" + operation;
    }

    private static String packageResultPath(BuildRegistration buildRegistration, String fileName) {
        return operationResultPath(buildRegistration, "package", fileName);
    }

    private static String packageResultDirectoryPath(BuildRegistration buildRegistration) {
        return "quarkus-build-results/" + buildRegistration.name() + "/package";
    }

    static String operationResultPath(BuildRegistration buildRegistration, String operation, String fileName) {
        return "quarkus-build-results/" + buildRegistration.name() + "/" + operation + "/" + fileName;
    }

    private void configureNamedBuildTask(Project project, QuarkusApplicationExtension extension,
            QuarkusApplicationBuildTask task, BuildRegistration buildRegistration) {
        configureNamedBuildTask(project, extension, task, buildRegistration, applicationModel);
    }

    static void configureNamedBuildTask(Project project, QuarkusApplicationExtension extension,
            QuarkusApplicationBuildTask task, BuildRegistration buildRegistration,
            TaskProvider<GenerateModelTask> applicationModel) {
        configureNamedEffectiveConfigTask(project, extension, task, buildRegistration);
        task.getGradleBuildDirectory().set(project.getLayout().getBuildDirectory());
        task.getApplicationModel().set(applicationModel.flatMap(GenerateModelTask::getApplicationModel));
        configureForkOptions(task.getBuildForkOptions(), extension.getBuildForkOptions());
        configureJavaClasspath(project, task);
    }

    private static void configureNamedEffectiveConfigTask(Project project, QuarkusApplicationExtension extension,
            QuarkusApplicationEffectiveConfigTask task, BuildRegistration buildRegistration) {
        configureNamedTask(task, buildRegistration, extension.getConfigInputs());
        configureAdditionalDescriptorShapeProperties(task, buildRegistration.build());
        configureManifestProperties(task, buildRegistration);
        task.getQuarkusBuildProperties().set(extension.getQuarkusBuildProperties());
        task.getQuarkusBuildProperties().putAll(buildRegistration.build().getQuarkusBuildProperties());
        task.getPackageOutputTimestamp().set(extension.getPackageOutputTimestamp());
        task.getApplicationName().set(project.getName());
        task.getApplicationVersion().set(project.getVersion().toString());
        configureJavaSourceDirectories(project, task);
    }

    private static void configureManifestProperties(QuarkusApplicationEffectiveConfigTask task,
            BuildRegistration buildRegistration) {
        if (!(buildRegistration.build() instanceof QuarkusApplicationJarOutput jarOutput)) {
            return;
        }
        String context = "named build '" + buildRegistration.name() + "' task '" + task.getPath() + "'";
        task.getManifestConfigProperties().putAll(jarOutput.getManifest().getAttributes()
                .map(attributes -> ManifestConfigProperties.attributes(context, attributes)));
        jarOutput.getManifest().getSections().all(section -> {
            String sectionName = section.getName();
            task.getManifestConfigProperties().putAll(section.getAttributes()
                    .map(attributes -> ManifestConfigProperties.section(context, sectionName, attributes)));
        });
    }

    private static void configureAdditionalDescriptorShapeProperties(QuarkusApplicationEffectiveConfigTask task,
            QuarkusApplicationBuild build) {
        if (build instanceof QuarkusApplicationRunnerOutput runnerOutput) {
            task.getAdditionalDescriptorShapeProperties().put("quarkus.package.runner-suffix",
                    runnerOutput.getArchiveRunnerSuffix());
            task.getAdditionalDescriptorShapeProperties().put("quarkus.package.jar.add-runner-suffix",
                    runnerOutput.getArchiveAddRunnerSuffix().map(String::valueOf));
        }
    }

    private static void configureNamedTask(QuarkusApplicationTask task, BuildRegistration buildRegistration,
            QuarkusApplicationConfigInputs configInputs) {
        task.getBuildName().set(buildRegistration.name());
        task.getBuildType().set(buildRegistration.type());
        task.getOutputName().set(buildRegistration.build().getOutputName());
        task.getOutputDirectory().set(buildRegistration.build().getOutputDirectory());
        configureConfigInputs(task, configInputs);
    }

    private static String buildDescription(BuildRegistration buildRegistration) {
        return switch (buildRegistration.type()) {
            case FAST_JAR, AOT_JAR, LEGACY_JAR, MUTABLE_JAR, UBER_JAR -> "Builds the '"
                    + buildRegistration.name() + "' "
                    + buildRegistration.type().jarType().orElseThrow() + " Quarkus application.";
            case NATIVE_EXECUTABLE -> "Builds the '" + buildRegistration.name() + "' native executable Quarkus application.";
            case NATIVE_SOURCES -> "Generates native-image sources for the '" + buildRegistration.name()
                    + "' Quarkus application.";
        };
    }

    private BuildRegistration validateNamedBuild(QuarkusApplicationBuild build) {
        var buildRegistration = new BuildRegistration(build);
        var previous = buildNames.putIfAbsent(buildRegistration.taskSegment().collisionKey(), buildRegistration);
        if (previous != null) {
            throw new GradleException(
                    "Quarkus application build names '" + previous.name() + "' and '" + buildRegistration.name()
                            + "' derive the same task-name segment");
        }
        return buildRegistration;
    }

    record BuildRegistration(
            QuarkusApplicationBuild build,
            QuarkusApplicationBuildDescriptor descriptor) {
        BuildRegistration(QuarkusApplicationBuild build) {
            this(build, new QuarkusApplicationBuildDescriptor(build.getName(), build.getBuildType()));
        }

        String name() {
            return descriptor.name();
        }

        QuarkusApplicationBuildType type() {
            return descriptor.type();
        }

        TaskNameSegment taskSegment() {
            return TaskNameSegment.of(name());
        }
    }
}
