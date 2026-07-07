package io.quarkus.gradle.application.internal.dev;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.gradle.api.GradleException;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.OperationCompletionListener;
import org.gradle.tooling.events.task.TaskFailureResult;
import org.gradle.tooling.events.task.TaskFinishEvent;

import io.quarkus.deployment.dev.BuildOutputFailureKind;

/**
 * Records public Gradle task-finish events for the finalizer task that owns the
 * build-session deployment-registry interaction. The service is build-scoped,
 * so the retained state is bounded to the task graph that consumes it.
 */
public abstract class QuarkusApplicationContinuousBuildFailureListener
        implements BuildService<QuarkusApplicationContinuousBuildFailureListener.Parameters>, OperationCompletionListener {

    private static final long EVENT_TIMEOUT_SECONDS = 30;

    private final Map<String, CompletableFuture<Boolean>> taskFailures = new ConcurrentHashMap<>();

    @Override
    public void onFinish(FinishEvent event) {
        if (!(event instanceof TaskFinishEvent taskEvent)) {
            return;
        }

        String taskPath = taskEvent.getDescriptor().getTaskPath();
        if (!getParameters().getFailureKinds().get().containsKey(taskPath)) {
            return;
        }
        taskFailures.computeIfAbsent(taskPath, ignored -> new CompletableFuture<>())
                .complete(taskEvent.getResult() instanceof TaskFailureResult);
    }

    public final BuildOutputFailureKind awaitFailure(String taskPath) {
        BuildOutputFailureKind failureKind = getParameters().getFailureKinds().get().get(taskPath);
        if (failureKind == null) {
            throw new GradleException("Task path '" + taskPath + "' is not configured for continuous-build failure reporting");
        }
        try {
            boolean failed = taskFailures.computeIfAbsent(taskPath, ignored -> new CompletableFuture<>())
                    .get(EVENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return failed ? failureKind : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GradleException("Interrupted while waiting for the completion event for Gradle task '" + taskPath + "'",
                    e);
        } catch (ExecutionException e) {
            throw new GradleException("Failed while waiting for the completion event for Gradle task '" + taskPath + "'",
                    e.getCause());
        } catch (TimeoutException e) {
            throw new GradleException("Timed out waiting for the completion event for Gradle task '" + taskPath + "'", e);
        }
    }

    public interface Parameters extends BuildServiceParameters {

        MapProperty<String, BuildOutputFailureKind> getFailureKinds();
    }
}
