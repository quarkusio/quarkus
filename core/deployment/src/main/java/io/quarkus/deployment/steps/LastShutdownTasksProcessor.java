package io.quarkus.deployment.steps;

import io.quarkus.core.deployment.action.ActionBuilder;
import io.quarkus.core.impl.LastShutdownTasks;
import io.quarkus.deployment.Phase;
import io.quarkus.deployment.annotations.BuildStep;

/**
 * Registers the static-init and runtime-init services that run global "last" shutdown tasks.
 */
public class LastShutdownTasksProcessor {

    /**
     * Runs global static-init "last" shutdown tasks collected via
     * {@link io.quarkus.runtime.ShutdownContext#addLastShutdownTask(Runnable)}.
     * <p>
     * The ArcContainer service declares {@code after("io.quarkus.core.last-shutdown-tasks")}
     * so it stops before this service — bean destruction (and OTel flush etc.)
     * complete before static "last" tasks (like SmallRye Context Propagation cleanup) run.
     *
     * @param action the action builder
     */
    @BuildStep
    void registerLastShutdownTasks(ActionBuilder action) {
        action
                .forService("io.quarkus.core.last-shutdown-tasks")
                .atPhase(Phase.STATIC_INIT)
                .action(ctx -> {
                    ctx.onStop(LastShutdownTasks::runStatic);
                });
    }

    /**
     * Runs global runtime-init "last" shutdown tasks collected via
     * {@link io.quarkus.runtime.ShutdownContext#addLastShutdownTask(Runnable)}.
     * <p>
     * This service is configured to run at the very end of the runtime-init
     * graph's stop cascade, immediately before the core thread pool shuts down.
     *
     * @param action the action builder
     */
    @BuildStep
    void registerRuntimeLastShutdownTasks(ActionBuilder action) {
        action
                .forService("io.quarkus.core.last-runtime-shutdown-tasks")
                .atPhase(Phase.INFRASTRUCTURE)
                .before(java.util.concurrent.ScheduledExecutorService.class)
                .action(ctx -> {
                    ctx.onStop(LastShutdownTasks::runRuntime);
                });
    }
}
