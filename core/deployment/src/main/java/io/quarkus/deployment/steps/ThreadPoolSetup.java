package io.quarkus.deployment.steps;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import io.quarkus.core.Phase;
import io.quarkus.core.deployment.action.ActionBuilder;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.QuarkusContextHandler;
import io.quarkus.runtime.ThreadPoolConfig;

/**
 * Registers the main thread pool as a service.
 */
public class ThreadPoolSetup {

    @BuildStep
    public ExecutorBuildItem createExecutor(ActionBuilder action,
            LaunchModeBuildItem launchModeBuildItem) {
        LaunchMode launchMode = launchModeBuildItem.getLaunchMode();
        action
                .forService(ScheduledExecutorService.class)
                .atPhase(Phase.INFRASTRUCTURE)
                .require(ThreadPoolConfig.class)
                .request(ThreadFactory.class)
                .request(QuarkusContextHandler.class)
                .action((ctx, config, threadFactoryOpt, contextHandlerOpt) -> {
                    ScheduledExecutorService executor = ExecutorRecorder.setupRunTime(
                            config,
                            launchMode,
                            threadFactoryOpt.orElse(null),
                            contextHandlerOpt.orElse(null));
                    ctx.onStop(() -> ExecutorRecorder.shutdownExecutor(config, launchMode));
                    return executor;
                });
        return new ExecutorBuildItem(action.getRecorderProxy(ScheduledExecutorService.class));
    }

    @BuildStep
    RuntimeInitializedClassBuildItem registerClasses() {
        // make sure that the config provider gets initialized only at run time
        return new RuntimeInitializedClassBuildItem(ExecutorRecorder.class.getName());
    }
}
