package io.quarkus.opentelemetry.runtime.logs;

import java.util.Optional;
import java.util.logging.Handler;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.arc.Arc;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class OpenTelemetryLogRecorder {
    public RuntimeValue<Optional<Handler>> initializeHandler(final OTelRuntimeConfig config) {
        if (config.sdkDisabled() || !config.logs().handlerEnabled()) {
            return new RuntimeValue<>(Optional.empty());
        }
        OpenTelemetry openTelemetry = Arc.container().instance(OpenTelemetry.class).get();
        return new RuntimeValue<>(Optional.of(new OpenTelemetryLogHandler(openTelemetry)));
    }
}
