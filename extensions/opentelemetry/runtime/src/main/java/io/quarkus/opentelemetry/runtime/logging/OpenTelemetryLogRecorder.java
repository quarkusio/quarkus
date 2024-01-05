package io.quarkus.opentelemetry.runtime.logging;

import java.util.Optional;
import java.util.logging.Handler;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class OpenTelemetryLogRecorder {
    public RuntimeValue<Optional<Handler>> initializeHandler(final OpenTelemetryLogConfig config) {
        if (!config.enabled) {
            return new RuntimeValue<>(Optional.empty());
        }

        OpenTelemetryLogHandler handler = new OpenTelemetryLogHandler(GlobalOpenTelemetry.get());

        return new RuntimeValue<>(Optional.of(handler));
    }
}
