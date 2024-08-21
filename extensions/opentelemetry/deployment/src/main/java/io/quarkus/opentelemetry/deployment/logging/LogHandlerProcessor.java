package io.quarkus.opentelemetry.deployment.logging;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

import io.quarkus.agroal.spi.OpenTelemetryInitBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.logs.OpenTelemetryLogConfig;
import io.quarkus.opentelemetry.runtime.logs.OpenTelemetryLogRecorder;

@BuildSteps(onlyIf = LogHandlerProcessor.LogsEnabled.class)
class LogHandlerProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(OpenTelemetryInitBuildItem.class)
    LogHandlerBuildItem build(OpenTelemetryLogRecorder recorder, OpenTelemetryLogConfig config) {
        return new LogHandlerBuildItem(recorder.initializeHandler(config));
    }

    public static class LogsEnabled implements BooleanSupplier {
        OTelBuildConfig otelBuildConfig;

        public boolean getAsBoolean() {
            return otelBuildConfig.logs().enabled()
                    .map(new Function<Boolean, Boolean>() {
                        @Override
                        public Boolean apply(Boolean enabled) {
                            return otelBuildConfig.enabled() && enabled;
                        }
                    })
                    .orElseGet(() -> otelBuildConfig.enabled());
        }
    }
}
