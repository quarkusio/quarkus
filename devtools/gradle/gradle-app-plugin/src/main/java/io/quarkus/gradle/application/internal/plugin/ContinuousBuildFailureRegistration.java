package io.quarkus.gradle.application.internal.plugin;

import java.util.LinkedHashMap;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.build.event.BuildEventsListenerRegistry;

import io.quarkus.deployment.dev.BuildOutputFailureKind;
import io.quarkus.gradle.application.internal.dev.QuarkusApplicationContinuousBuildFailureListener;
import io.quarkus.gradle.application.internal.dev.QuarkusApplicationContinuousBuildFailureTask;
import io.quarkus.gradle.application.internal.dev.QuarkusApplicationDevDeployments;

final class ContinuousBuildFailureRegistration {

    private final BuildEventsListenerRegistry buildEventsListeners;

    ContinuousBuildFailureRegistration(BuildEventsListenerRegistry buildEventsListeners) {
        this.buildEventsListeners = buildEventsListeners;
    }

    void register(Project project, String sessionTaskName, boolean explicitlyRequested) {
        // Listener services and producer finalizers belong only to a requested
        // continuous session; registering them for ordinary builds would instrument
        // unrelated task executions and keep an unused session service alive.
        if (!explicitlyRequested) {
            return;
        }
        String deploymentId = QuarkusApplicationDevDeployments.deploymentId(
                project.getProjectDir().toPath(), taskPath(project, sessionTaskName));
        Map<String, BuildOutputFailureKind> failureKinds = failureKinds(project);
        Provider<QuarkusApplicationContinuousBuildFailureListener> listener = project.getGradle().getSharedServices()
                .registerIfAbsent("quarkusApplicationContinuousBuildFailure-" + deploymentId,
                        QuarkusApplicationContinuousBuildFailureListener.class, spec -> {
                            spec.getParameters().getFailureKinds().set(failureKinds);
                        });
        buildEventsListeners.onTaskCompletion(listener);
        failureKinds.keySet().forEach(producerTaskPath -> registerFailureReporter(
                project, sessionTaskName, producerTaskPath, deploymentId, listener));
    }

    private static void registerFailureReporter(Project project, String sessionTaskName, String producerTaskPath,
            String deploymentId, Provider<QuarkusApplicationContinuousBuildFailureListener> listener) {
        String producerTaskName = producerTaskPath.substring(producerTaskPath.lastIndexOf(':') + 1);
        String reporterName = sessionTaskName + Character.toUpperCase(producerTaskName.charAt(0))
                + producerTaskName.substring(1) + "FailureReporter";
        TaskProvider<QuarkusApplicationContinuousBuildFailureTask> reporter = project.getTasks().register(reporterName,
                QuarkusApplicationContinuousBuildFailureTask.class, task -> {
                    task.getProducerTaskPath().set(producerTaskPath);
                    task.getDeploymentId().set(deploymentId);
                    task.getFailureListener().set(listener);
                    task.usesService(listener);
                    task.notCompatibleWithConfigurationCache(
                            "Failure reporting is part of the configuration-cache-incompatible continuous session.");
                });
        // Finalizers still run after a producer failure. The listener records the
        // completion result and the reporter forwards that failure to the live child.
        project.getTasks().matching(task -> task.getName().equals(producerTaskName))
                .configureEach(task -> task.finalizedBy(reporter));
    }

    private static Map<String, BuildOutputFailureKind> failureKinds(Project project) {
        Map<String, BuildOutputFailureKind> failureKinds = new LinkedHashMap<>();
        addFailureKind(project, failureKinds, "quarkusApplicationGenerateCode", BuildOutputFailureKind.MAIN);
        addFailureKind(project, failureKinds, "quarkusApplicationGenerateDevCode", BuildOutputFailureKind.MAIN);
        addFailureKind(project, failureKinds, "quarkusApplicationDevModel", BuildOutputFailureKind.MAIN);
        addFailureKind(project, failureKinds, "compileJava", BuildOutputFailureKind.MAIN);
        addFailureKind(project, failureKinds, "compileKotlin", BuildOutputFailureKind.MAIN);
        addFailureKind(project, failureKinds, "kaptKotlin", BuildOutputFailureKind.MAIN);
        addFailureKind(project, failureKinds, "kspKotlin", BuildOutputFailureKind.MAIN);
        addFailureKind(project, failureKinds, JavaPlugin.PROCESS_RESOURCES_TASK_NAME, BuildOutputFailureKind.MAIN);
        addFailureKind(project, failureKinds, "quarkusApplicationGenerateTestCode", BuildOutputFailureKind.TEST);
        addFailureKind(project, failureKinds, "quarkusApplicationContinuousTestModel", BuildOutputFailureKind.TEST);
        addFailureKind(project, failureKinds, "compileTestJava", BuildOutputFailureKind.TEST);
        addFailureKind(project, failureKinds, "compileTestKotlin", BuildOutputFailureKind.TEST);
        addFailureKind(project, failureKinds, "kaptTestKotlin", BuildOutputFailureKind.TEST);
        addFailureKind(project, failureKinds, "kspTestKotlin", BuildOutputFailureKind.TEST);
        addFailureKind(project, failureKinds, JavaPlugin.PROCESS_TEST_RESOURCES_TASK_NAME, BuildOutputFailureKind.TEST);
        return failureKinds;
    }

    private static void addFailureKind(Project project, Map<String, BuildOutputFailureKind> failureKinds,
            String taskName, BuildOutputFailureKind failureKind) {
        failureKinds.put(taskPath(project, taskName), failureKind);
    }

    private static String taskPath(Project project, String taskName) {
        return project.getPath().equals(":") ? ":" + taskName : project.getPath() + ":" + taskName;
    }
}
