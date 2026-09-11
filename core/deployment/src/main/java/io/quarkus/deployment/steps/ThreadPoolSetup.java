package io.quarkus.deployment.steps;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.jboss.threads.ContextHandler;

import io.quarkus.core.deployment.action.ActionBuilder;
import io.quarkus.deployment.Phase;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.runtime.ExecutorRecorder;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ThreadPoolConfig;

/**
 * Registers the main thread pool as a service.
 */
public class ThreadPoolSetup {

    @BuildStep
    @SuppressWarnings("unchecked")
    public ExecutorBuildItem createExecutor(ActionBuilder action,
            LaunchModeBuildItem launchModeBuildItem) {
        LaunchMode launchMode = launchModeBuildItem.getLaunchMode();
        action
                .forService(ScheduledExecutorService.class)
                .atPhase(Phase.INFRASTRUCTURE)
                .require(ThreadPoolConfig.class)
                .request(ThreadFactory.class)
                .request(ContextHandler.class, "io.quarkus.vertx.context-handler")
                .action((ctx, config, threadFactoryOpt, contextHandlerOpt) -> {
                    ScheduledExecutorService executor = ExecutorRecorder.setupRunTime(
                            config,
                            launchMode,
                            ((Optional<ThreadFactory>) threadFactoryOpt).orElse(null),
                            ((Optional<ContextHandler<Object>>) (Optional<?>) contextHandlerOpt).orElse(null));
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
