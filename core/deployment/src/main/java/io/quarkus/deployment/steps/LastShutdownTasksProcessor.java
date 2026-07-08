package io.quarkus.deployment.steps;

import io.quarkus.core.deployment.action.ActionBuilder;
import io.quarkus.core.impl.LastShutdownTasks;
import io.quarkus.deployment.Phase;
import io.quarkus.deployment.annotations.BuildStep;

/**
 * Registers the static-init service that runs global "last" shutdown tasks.
 */
public class LastShutdownTasksProcessor {

    /**
     * Runs global "last" shutdown tasks collected via
     * {@link io.quarkus.runtime.ShutdownContext#addLastShutdownTask(Runnable)}.
     * <p>
     * The ArcContainer service declares {@code after("io.quarkus.core.last-shutdown-tasks")}
     * so it stops before this service — bean destruction (and OTel flush etc.)
     * complete before "last" tasks (like SmallRye Context Propagation cleanup) run.
     */
    @BuildStep
    void registerLastShutdownTasks(ActionBuilder action) {
        action
                .forService("io.quarkus.core.last-shutdown-tasks")
                .atPhase(Phase.STATIC_INIT)
                .action(ctx -> {
                    ctx.onStop(LastShutdownTasks::run);
                });
    }
}
