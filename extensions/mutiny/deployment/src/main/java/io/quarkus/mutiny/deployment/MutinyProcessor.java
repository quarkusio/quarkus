package io.quarkus.mutiny.deployment;

import java.util.concurrent.ScheduledExecutorService;

import io.quarkus.core.deployment.action.ActionBuilder;
import io.quarkus.deployment.Phase;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.mutiny.runtime.MutinyInfrastructure;

/**
 * Build steps for configuring the Mutiny infrastructure.
 */
public class MutinyProcessor {

    /**
     * Configure the Mutiny infrastructure with the application's executor at runtime.
     * <p>
     * The {@code executorBuildItem} parameter is consumed (but not used directly) to ensure
     * that the build step which aliases the executor into the service graph runs before this one.
     *
     * @param action the action builder
     * @param executorBuildItem consumed for build-step ordering (the executor value is obtained
     *        via the service graph)
     * @return a build item signaling that Mutiny runtime initialization is complete
     */
    @BuildStep
    @SuppressWarnings("unused")
    MutinyRuntimeInitBuildItem runtimeInit(ActionBuilder action, ExecutorBuildItem executorBuildItem) {
        action
                .forService("io.quarkus.mutiny.runtime-init")
                .atPhase(Phase.INFRASTRUCTURE)
                .require(ScheduledExecutorService.class)
                .action((ctx, executor) -> MutinyInfrastructure.configureMutinyInfrastructure(executor));

        return new MutinyRuntimeInitBuildItem();
    }

    /**
     * Configure Mutiny's dropped exception handler, thread blocking checker,
     * and operator logger at static init time.
     *
     * @param action the action builder
     */
    @BuildStep
    void buildTimeInit(ActionBuilder action) {
        action
                .forService("io.quarkus.mutiny.static-init")
                .atPhase(Phase.STATIC_INIT)
                .action(ctx -> {
                    MutinyInfrastructure.configureDroppedExceptionHandler();
                    MutinyInfrastructure.configureThreadBlockingChecker();
                    MutinyInfrastructure.configureOperatorLogger();
                });
    }
}
