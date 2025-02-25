package io.quarkus.opentelemetry.runtime.config.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigGroup
public interface LogsRuntimeConfig {

    /**
     * Determine whether to enable the OpenTelemetry logging handler.
     * <p>
     * This is a Quarkus specific property. The OpenTelemetry logging handler is enabled by default.
     */
    @WithDefault("true")
    @WithName("handler.enabled")
    boolean handlerEnabled();
}
