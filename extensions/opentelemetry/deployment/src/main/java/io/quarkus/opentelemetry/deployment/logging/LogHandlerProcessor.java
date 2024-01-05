package io.quarkus.opentelemetry.deployment.logging;

import io.quarkus.agroal.spi.OpenTelemetryInitBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.opentelemetry.runtime.logs.OpenTelemetryLogConfig;
import io.quarkus.opentelemetry.runtime.logs.OpenTelemetryLogRecorder;

class LogHandlerProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(OpenTelemetryInitBuildItem.class)
    LogHandlerBuildItem build(OpenTelemetryLogRecorder recorder, OpenTelemetryLogConfig config) {
        return new LogHandlerBuildItem(recorder.initializeHandler(config));
    }
}
