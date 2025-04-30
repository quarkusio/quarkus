package io.quarkus.opentelemetry.runtime.config.runtime;

import java.util.logging.Level;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.logging.LevelConverter;
import io.smallrye.config.WithConverter;
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

    /**
     * The log level to use for the OpenTelemetry logging handler.
     * <p>
     * This is a Quarkus specific property. The default log level is ALL.
     */
    @WithDefault("ALL")
    @WithConverter(LevelConverter.class)
    Level level();
}
