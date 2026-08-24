package io.quarkus.opentelemetry.deployment.logging;

import java.util.Optional;
import java.util.logging.Handler;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Carries the OpenTelemetry log handler so it can be activated once the OpenTelemetry SDK
 * is available, after having been installed during early logging setup.
 */
public final class OpenTelemetryLogHandlerBuildItem extends SimpleBuildItem {

    private final RuntimeValue<Optional<Handler>> handler;

    public OpenTelemetryLogHandlerBuildItem(RuntimeValue<Optional<Handler>> handler) {
        this.handler = handler;
    }

    public RuntimeValue<Optional<Handler>> getHandler() {
        return handler;
    }
}
