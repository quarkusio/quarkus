package io.quarkus.arc.deployment.init;

import java.util.List;

import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.InitTaskCompletedBuildItem;
import io.quarkus.runtime.init.InitializationTaskRecorder;

/**
 * A processor that is used to track all {@link io.quarkus.deployment.builditem.InitTaskCompletedBuildItem} in order to
 * exit once they are completed if needed.
 */
public class InitializationTaskProcessor {

    @BuildStep
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void startApplicationInitializer(InitializationTaskRecorder recorder,
            List<InitTaskCompletedBuildItem> initTaskCompletedBuildItems) {
        recorder.exitIfNeeded();
    }
}
