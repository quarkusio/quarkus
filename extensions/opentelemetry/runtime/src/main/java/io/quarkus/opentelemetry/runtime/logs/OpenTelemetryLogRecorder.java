package io.quarkus.opentelemetry.runtime.logs;

import java.util.Optional;
import java.util.logging.Handler;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class OpenTelemetryLogRecorder {
    private final RuntimeValue<OTelRuntimeConfig> runtimeConfig;

    public OpenTelemetryLogRecorder(final RuntimeValue<OTelRuntimeConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public RuntimeValue<Optional<Handler>> initializeHandler(final BeanContainer beanContainer) {
        if (runtimeConfig.getValue().sdkDisabled() || !runtimeConfig.getValue().logs().handlerEnabled()) {
            return new RuntimeValue<>(Optional.empty());
        }
        final OpenTelemetry openTelemetry = beanContainer.beanInstance(OpenTelemetry.class);
        final OpenTelemetryLogHandler logHandler = new OpenTelemetryLogHandler(openTelemetry);
        logHandler.setLevel(runtimeConfig.getValue().logs().level());
        return new RuntimeValue<>(Optional.of(logHandler));
    }
}
