package io.quarkus.opentelemetry.runtime.config.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface InstrumentRuntimeConfig {

    /**
     * Enables instrumentation for Vert.x HTTP.
     */
    @WithDefault("true")
    boolean vertxHttp();

    /**
     * Enables instrumentation for Vert.x Event Bus.
     */
    @WithDefault("true")
    boolean vertxEventBus();

    /**
     * Enables instrumentation for Vert.x SQL Client.
     */
    @WithDefault("true")
    boolean vertxSqlClient();

    /**
     * Enables instrumentation for Vert.x Redis Client.
     */
    @WithDefault("true")
    boolean vertxRedisClient();

    /**
     * Enables instrumentation for JVM Metrics.
     */
    @WithDefault("true")
    boolean jvmMetrics();

    /**
     * Enables instrumentation for HTTP Server Metrics.
     */
    @WithDefault("true")
    boolean httpServerMetrics();
}
