package io.quarkus.gradle.application.internal.plugin;

import static io.quarkus.gradle.application.internal.plugin.TaskRegistrationSupport.QUARKUS_APPLICATION_GROUP;
import static io.quarkus.gradle.application.internal.plugin.TaskRegistrationSupport.configureConfigInputs;
import static io.quarkus.gradle.application.internal.plugin.TaskRegistrationSupport.configureForkOptions;
import static io.quarkus.gradle.application.internal.plugin.TaskRegistrationSupport.configureJavaClasspath;
import static io.quarkus.gradle.application.internal.plugin.TaskRegistrationSupport.configureJavaSourceDirectories;

import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

import io.quarkus.gradle.application.dsl.QuarkusApplicationExtension;
import io.quarkus.gradle.application.dsl.QuarkusApplicationForkOptions;
import io.quarkus.gradle.application.internal.modelgen.GenerateModelTask;
import io.quarkus.gradle.application.internal.planning.PackageOutputName;
import io.quarkus.gradle.application.internal.remotedev.QuarkusApplicationRemoteDevReconnectTriggerTask;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.gradle.application.model.QuarkusApplicationLaunchKind;
import io.quarkus.gradle.application.tasks.QuarkusApplicationPackageTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationRemoteDevTask;

final class RemoteDevTaskRegistration {

    private static final String REMOTE_DEV_BUILD_NAME = "remoteDev";
    private static final String REMOTE_DEV_BUILD_TASK_NAME = "quarkusApplicationRemoteDevBuild";
    private static final String REMOTE_DEV_RECONNECT_TRIGGER_TASK_NAME = "quarkusApplicationRemoteDevInitializeReconnectTrigger";
    private static final String REMOTE_DEV_TASK_NAME = "quarkusApplicationRemoteDev";

    private final TaskNameRegistry taskNames;

    RemoteDevTaskRegistration(TaskNameRegistry taskNames) {
        this.taskNames = taskNames;
    }

    void register(Project project, QuarkusApplicationExtension extension,
            TaskProvider<GenerateModelTask> applicationModel) {
        // The long-lived remote session consumes a mutable-jar producer and a
        // build-owned trigger whose changes request the next continuous iteration.
        TaskProvider<QuarkusApplicationRemoteDevReconnectTriggerTask> reconnectTrigger = registerReconnectTrigger(project);
        TaskProvider<QuarkusApplicationPackageTask> remoteDevBuild = registerPackageTask(project, extension,
                applicationModel);
        registerRemoteDevTask(project, extension, remoteDevBuild, reconnectTrigger);
    }

    private TaskProvider<QuarkusApplicationRemoteDevReconnectTriggerTask> registerReconnectTrigger(Project project) {
        taskNames.register(project, REMOTE_DEV_RECONNECT_TRIGGER_TASK_NAME);
        return project.getTasks().register(REMOTE_DEV_RECONNECT_TRIGGER_TASK_NAME,
                QuarkusApplicationRemoteDevReconnectTriggerTask.class,
                task -> task.getTriggerFile().set(project.getLayout().getBuildDirectory()
                        .file("quarkus-remote-dev/reconnect/reconnect.trigger")));
    }

    private TaskProvider<QuarkusApplicationPackageTask> registerPackageTask(Project project,
            QuarkusApplicationExtension extension, TaskProvider<GenerateModelTask> applicationModel) {
        taskNames.register(project, REMOTE_DEV_BUILD_TASK_NAME);
        return project.getTasks().register(REMOTE_DEV_BUILD_TASK_NAME, QuarkusApplicationPackageTask.class, task -> {
            configurePackageTask(project, extension, applicationModel, task);
            task.setGroup(QUARKUS_APPLICATION_GROUP);
            task.setDescription("Builds the internal mutable-jar package used by Gradle-native Quarkus remote dev.");
        });
    }

    private static void configurePackageTask(Project project, QuarkusApplicationExtension extension,
            TaskProvider<GenerateModelTask> applicationModel, QuarkusApplicationPackageTask task) {
        task.getBuildName().set(REMOTE_DEV_BUILD_NAME);
        task.getBuildType().set(QuarkusApplicationBuildType.MUTABLE_JAR);
        task.getOutputName().set(project.provider(() -> PackageOutputName.assemble(project.getName(), "",
                project.getVersion().toString())));
        task.getOutputDirectory().set(project.getLayout().getBuildDirectory().dir("quarkus-remote-dev/build"));
        task.getQuarkusBuildProperties().set(extension.getQuarkusBuildProperties());
        task.getQuarkusBuildProperties().putAll(extension.getRemoteDev().getQuarkusBuildProperties());
        task.getPackageOutputTimestamp().set(extension.getPackageOutputTimestamp());
        task.getApplicationName().set(project.getName());
        task.getApplicationVersion().set(project.getVersion().toString());
        task.getGradleBuildDirectory().set(project.getLayout().getBuildDirectory());
        task.getApplicationModel().set(applicationModel.flatMap(GenerateModelTask::getApplicationModel));
        task.getPackageResultFile().set(project.getLayout().getBuildDirectory()
                .file("quarkus-remote-dev/build-result/package-result.properties"));
        configureForkOptions(task.getBuildForkOptions(), extension.getBuildForkOptions());
        configureRemoteDevForkOptions(task.getBuildForkOptions(), extension);
        configureJavaClasspath(project, task);
        configureJavaSourceDirectories(project, task);
        configureConfigInputs(task, extension.getConfigInputs());
    }

    private void registerRemoteDevTask(Project project, QuarkusApplicationExtension extension,
            TaskProvider<QuarkusApplicationPackageTask> remoteDevBuild,
            TaskProvider<QuarkusApplicationRemoteDevReconnectTriggerTask> reconnectTrigger) {
        taskNames.register(project, REMOTE_DEV_TASK_NAME);
        project.getTasks().register(REMOTE_DEV_TASK_NAME, QuarkusApplicationRemoteDevTask.class, task -> {
            task.dependsOn(remoteDevBuild, reconnectTrigger);
            task.getReconnectTriggerFile()
                    .set(reconnectTrigger.flatMap(QuarkusApplicationRemoteDevReconnectTriggerTask::getTriggerFile));
            task.getLaunchKind().set(QuarkusApplicationLaunchKind.REMOTE_DEV);
            task.getBuildName().set(REMOTE_DEV_BUILD_NAME);
            task.getBuildType().set(QuarkusApplicationBuildType.MUTABLE_JAR);
            task.getOutputName().set(remoteDevBuild.flatMap(QuarkusApplicationPackageTask::getOutputName));
            task.getOutputDirectory().set(remoteDevBuild.flatMap(QuarkusApplicationPackageTask::getOutputDirectory));
            task.getContinuousBuild().set(project.getGradle().getStartParameter().isContinuous());
            task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
            task.getQuarkusBuildProperties().set(extension.getQuarkusBuildProperties());
            task.getQuarkusBuildProperties().putAll(extension.getRemoteDev().getQuarkusBuildProperties());
            task.getPackageResultFile().set(remoteDevBuild.flatMap(QuarkusApplicationPackageTask::getPackageResultFile));
            task.getPackageOutputDirectory().set(remoteDevBuild.flatMap(QuarkusApplicationPackageTask::getOutputDirectory));
            task.getReceiptFile().set(project.getLayout().getBuildDirectory()
                    .file("quarkus-remote-dev/build-result/remote-dev-result.properties"));
            task.getPackageSnapshotFile().set(project.getLayout().getBuildDirectory()
                    .file("quarkus-remote-dev/snapshot/package-snapshot.tsv"));
            task.getCloseReceiptFile().set(project.getLayout().getBuildDirectory()
                    .file("quarkus-remote-dev/snapshot/session-closed.txt"));
            configureConfigInputs(task, extension.getConfigInputs());
            task.setGroup(QUARKUS_APPLICATION_GROUP);
            task.setDescription("Runs Gradle-native Quarkus remote dev using an internal mutable-jar package.");
            task.notCompatibleWithConfigurationCache(
                    "Gradle-native Quarkus remote dev requires Gradle continuous build and keeps a long-lived remote session.");
        });
    }

    private static void configureRemoteDevForkOptions(QuarkusApplicationForkOptions target,
            QuarkusApplicationExtension extension) {
        target.getJvmArgs().addAll(extension.getRemoteDev().getForkOptions().getJvmArgs());
        target.getSystemProperties().putAll(extension.getRemoteDev().getForkOptions().getSystemProperties());
    }
}
