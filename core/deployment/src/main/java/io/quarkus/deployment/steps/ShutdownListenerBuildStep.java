package io.quarkus.deployment.steps;

import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ShutdownListenerBuildItem;
import io.quarkus.runtime.shutdown.ShutdownRecorder;

public class ShutdownListenerBuildStep {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupShutdown(List<ShutdownListenerBuildItem> listeners, ShutdownRecorder recorder) {
        recorder.setListeners(
                listeners.stream().map(ShutdownListenerBuildItem::getShutdownListener).collect(Collectors.toList()));
    }
}
