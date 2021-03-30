package io.quarkus.mutiny.deployment;

import java.util.concurrent.ExecutorService;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.mutiny.runtime.MutinyInfrastructure;

public class MutinyProcessor {

    @BuildStep
    public FeatureBuildItem registerFeature() {
        return new FeatureBuildItem(Feature.MUTINY);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void runtimeInit(ExecutorBuildItem executorBuildItem, MutinyInfrastructure recorder) {
        ExecutorService executor = executorBuildItem.getExecutorProxy();
        recorder.configureMutinyInfrastructure(executor);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void buildTimeInit(MutinyInfrastructure recorder) {
        recorder.configureDroppedExceptionHandler();
        recorder.configureThreadBlockingChecker();
        recorder.configureOperatorLogger();
    }
}
