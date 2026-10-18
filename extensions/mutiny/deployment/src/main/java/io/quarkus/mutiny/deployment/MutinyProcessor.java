package io.quarkus.mutiny.deployment;

import java.util.concurrent.ScheduledExecutorService;

import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
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
     * @param reg the service registrar
     * @param executorBuildItem consumed for build-step ordering (the executor value is obtained
     *        via the service graph)
     * @return a build item signaling that Mutiny runtime initialization is complete
     */
    @BuildStep
    @SuppressWarnings("unused")
    MutinyRuntimeInitBuildItem runtimeInit(ServiceRegistrar reg, ExecutorBuildItem executorBuildItem) {
        reg
                .forService("io.quarkus.mutiny.runtime-init")
                .atPhase(Phase.INFRASTRUCTURE)
                .require(ScheduledExecutorService.class)
                .onStart((ctx, executor) -> MutinyInfrastructure.configureMutinyInfrastructure(executor));

        return new MutinyRuntimeInitBuildItem();
    }

    /**
     * Configure Mutiny's dropped exception handler, thread blocking checker,
     * and operator logger at static init time.
     *
     * @param reg the service registrar
     */
    @BuildStep
    void buildTimeInit(ServiceRegistrar reg) {
        reg
                .forService("io.quarkus.mutiny.static-init")
                .atPhase(Phase.STATIC_INIT)
                .onStart(ctx -> {
                    MutinyInfrastructure.configureDroppedExceptionHandler();
                    MutinyInfrastructure.configureThreadBlockingChecker();
                    MutinyInfrastructure.configureOperatorLogger();
                });
    }
}
