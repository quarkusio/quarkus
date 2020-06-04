package io.quarkus.mutiny.deployment;

import java.util.concurrent.ExecutorService;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.mutiny.runtime.MutinyInfrastructure;

public class MutinyProcessor {

    @BuildStep
    public FeatureBuildItem registerFeature() {
        return new FeatureBuildItem(FeatureBuildItem.MUTINY);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void initExecutor(ExecutorBuildItem executorBuildItem, MutinyInfrastructure recorder) {
        ExecutorService executor = executorBuildItem.getExecutorProxy();
        recorder.configureMutinyInfrastructure(executor);
    }
}
