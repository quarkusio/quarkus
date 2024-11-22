package io.quarkus.deployment.steps;

import java.util.List;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ShutdownListenerBuildItem;
import io.quarkus.deployment.shutdown.ShutdownBuildTimeConfig;
import io.quarkus.runtime.shutdown.ShutdownRecorder;

public class ShutdownListenerBuildStep {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupShutdown(List<ShutdownListenerBuildItem> listeners, ShutdownBuildTimeConfig shutdownBuildTimeConfig,
            ShutdownRecorder recorder) {
        recorder.setListeners(listeners.stream().map(ShutdownListenerBuildItem::getShutdownListener).toList(),
                shutdownBuildTimeConfig.isDelayEnabled());
    }
}
