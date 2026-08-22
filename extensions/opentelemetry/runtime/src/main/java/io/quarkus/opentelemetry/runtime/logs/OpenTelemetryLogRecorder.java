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

    /**
     * Creates the handler without touching the CDI container, so it can be installed during
     * early logging setup. Making logging setup wait for the OpenTelemetry SDK would leave
     * all loggers running at the initial (all-enabling) level while the rest of runtime
     * initialization — CDI startup, eagerly started services — executes, see
     * <a href="https://github.com/quarkusio/quarkus/issues/55889">GitHub issue #55889</a>.
     * The handler buffers published records until {@link #activateHandler} provides the SDK.
     */
    public RuntimeValue<Optional<Handler>> initializeHandler() {
        if (runtimeConfig.getValue().sdkDisabled() || !runtimeConfig.getValue().logs().handlerEnabled()) {
            return new RuntimeValue<>(Optional.empty());
        }
        final OpenTelemetryLogHandler logHandler = new OpenTelemetryLogHandler();
        logHandler.setLevel(runtimeConfig.getValue().logs().level());
        return new RuntimeValue<>(Optional.of(logHandler));
    }

    public void activateHandler(final RuntimeValue<Optional<Handler>> handler, final BeanContainer beanContainer) {
        if (handler.getValue().isEmpty()) {
            return;
        }
        final OpenTelemetry openTelemetry = beanContainer.beanInstance(OpenTelemetry.class);
        ((OpenTelemetryLogHandler) handler.getValue().get()).activate(openTelemetry);
    }
}
