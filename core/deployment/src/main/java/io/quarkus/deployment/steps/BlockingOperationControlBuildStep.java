package io.quarkus.deployment.steps;

import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.IOThreadDetectorBuildItem;
import io.quarkus.runtime.BlockingOperationRecorder;

public class BlockingOperationControlBuildStep {

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    public void blockingOP(List<IOThreadDetectorBuildItem> threadDetectors,
            BlockingOperationRecorder recorder) {
        recorder
                .control(threadDetectors.stream().map(IOThreadDetectorBuildItem::getDetector).collect(Collectors.toList()));
    }
}
