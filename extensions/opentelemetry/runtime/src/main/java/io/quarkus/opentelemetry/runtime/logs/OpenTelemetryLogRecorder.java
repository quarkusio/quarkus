package io.quarkus.opentelemetry.runtime.logs;

import java.util.Optional;
import java.util.logging.Handler;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class OpenTelemetryLogRecorder {
    public RuntimeValue<Optional<Handler>> initializeHandler(final OpenTelemetryLogConfig config) {
        if (!config.enabled()) {
            return new RuntimeValue<>(Optional.empty());
        }

        return new RuntimeValue<>(Optional.of(new OpenTelemetryLogHandler()));
    }
}
