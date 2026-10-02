package io.quarkus.gradle.application.internal.dev;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.deployment.internal.DeploymentRegistry;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.deployment.dev.BuildOutputFailureKind;

/**
 * Synchronizes Gradle's asynchronous task-finish event with failure delivery
 * to the build-session-scoped dev deployment. The action deliberately consumes
 * only listener state and never looks up or inspects the producer task.
 */
@DisableCachingByDefault(because = "Reports failure state from a continuous-build task event")
public abstract class QuarkusApplicationContinuousBuildFailureTask extends DefaultTask {

    @Input
    public abstract Property<String> getProducerTaskPath();

    @Input
    public abstract Property<String> getDeploymentId();

    @Internal
    public abstract Property<QuarkusApplicationContinuousBuildFailureListener> getFailureListener();

    @Inject
    public abstract DeploymentRegistry getDeploymentRegistry();

    @TaskAction
    public void reportFailure() {
        String producerTaskPath = getProducerTaskPath().get();
        BuildOutputFailureKind failureKind = getFailureListener().get().awaitFailure(producerTaskPath);
        if (failureKind == null) {
            return;
        }
        QuarkusApplicationDevDeploymentHandle handle = getDeploymentRegistry().get(
                getDeploymentId().get(), QuarkusApplicationDevDeploymentHandle.class);
        if (handle != null && handle.reportBuildFailure(failureKind, producerTaskPath)) {
            getLogger().lifecycle("Reported failure of Gradle task '{}' to the Quarkus continuous session.",
                    producerTaskPath);
        }
    }
}
