package io.quarkus.mutiny.deployment;

import java.util.Optional;

import org.jboss.threads.ContextHandler;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ContextHandlerBuildItem;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.mutiny.runtime.MutinyInfrastructure;

public class MutinyProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void runtimeInit(ExecutorBuildItem executorBuildItem, MutinyInfrastructure recorder,
            ShutdownContextBuildItem shutdownContext, Optional<ContextHandlerBuildItem> contextHandler) {
        ContextHandler<Object> handler = contextHandler.map(ContextHandlerBuildItem::contextHandler).orElse(null);
        recorder.configureMutinyInfrastructure(executorBuildItem.getExecutorProxy(), shutdownContext, handler);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void buildTimeInit(MutinyInfrastructure recorder) {
        recorder.configureDroppedExceptionHandler();
        recorder.configureThreadBlockingChecker();
        recorder.configureOperatorLogger();
    }
}
