package io.quarkus.deployment.steps;

import java.util.concurrent.ScheduledExecutorService;

import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.core.impl.LastShutdownTasks;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.runtime.ShutdownContext;

/**
 * Registers the static-init and runtime-init services that run global "last" shutdown tasks.
 */
public class LastShutdownTasksProcessor {

    /**
     * Runs global static-init "last" shutdown tasks collected via
     * {@link ShutdownContext#addLastShutdownTask(Runnable)}.
     * <p>
     * The ArcContainer service declares {@code after("io.quarkus.core.last-shutdown-tasks")}
     * so it stops before this service — bean destruction (and OTel flush etc.)
     * complete before static "last" tasks (like SmallRye Context Propagation cleanup) run.
     *
     * @param reg the service registrar
     */
    @BuildStep
    void registerLastShutdownTasks(ServiceRegistrar reg) {
        reg
                .forService("io.quarkus.core.last-shutdown-tasks")
                .atPhase(Phase.STATIC_INIT)
                .onStart(ctx -> {
                    ctx.onStop(LastShutdownTasks::runStatic);
                });
    }

    /**
     * Runs global runtime-init "last" shutdown tasks collected via
     * {@link ShutdownContext#addLastShutdownTask(Runnable)}.
     * <p>
     * This service is configured to run at the very end of the runtime-init
     * graph's stop cascade, immediately before the core thread pool shuts down.
     *
     * @param reg the service registrar
     */
    @BuildStep
    void registerRuntimeLastShutdownTasks(ServiceRegistrar reg) {
        reg
                .forService("io.quarkus.core.last-runtime-shutdown-tasks")
                .atPhase(Phase.INFRASTRUCTURE)
                .before(ScheduledExecutorService.class)
                .onStart(ctx -> {
                    ctx.onStop(LastShutdownTasks::runRuntime);
                });
    }
}
