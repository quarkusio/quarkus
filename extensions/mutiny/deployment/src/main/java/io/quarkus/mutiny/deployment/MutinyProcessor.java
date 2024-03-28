package io.quarkus.mutiny.deployment;

import java.util.List;
import java.util.Optional;

import org.jboss.threads.ContextHandler;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ContextHandlerBuildItem;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.UnsafeAccessedFieldBuildItem;
import io.quarkus.mutiny.runtime.MutinyInfrastructure;

public class MutinyProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void runtimeInit(ExecutorBuildItem executorBuildItem,
            MutinyInfrastructure recorder,
            ShutdownContextBuildItem shutdownContext,
            Optional<ContextHandlerBuildItem> contextHandler) {
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

    @BuildStep
    public List<UnsafeAccessedFieldBuildItem> jctoolsUnsafeAccessedFields() {
        return List.of(
                new UnsafeAccessedFieldBuildItem(
                        "org.jctools.util.UnsafeRefArrayAccess",
                        "REF_ELEMENT_SHIFT"));
    }
}
