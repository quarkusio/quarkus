package io.quarkus.opentelemetry.deployment.logging;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.opentelemetry.runtime.logging.OpenTelemetryLogConfig;
import io.quarkus.opentelemetry.runtime.logging.OpenTelemetryLogRecorder;

class OpenTelemetryLogHandlerProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    LogHandlerBuildItem build(OpenTelemetryLogRecorder recorder, OpenTelemetryLogConfig config) {
        return new LogHandlerBuildItem(recorder.initializeHandler(config));
    }
}
