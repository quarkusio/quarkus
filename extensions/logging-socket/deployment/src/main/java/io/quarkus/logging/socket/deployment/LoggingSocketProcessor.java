package io.quarkus.logging.socket.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.logging.socket.SocketConfig;
import io.quarkus.logging.socket.SocketLogHandlerRecorder;

class LoggingSocketProcessor {

    private static final String FEATURE = "logging-socket";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    LogHandlerBuildItem build(SocketLogHandlerRecorder recorder, SocketConfig config) {
        return new LogHandlerBuildItem(recorder.initializeHandler(config));
    }

}
