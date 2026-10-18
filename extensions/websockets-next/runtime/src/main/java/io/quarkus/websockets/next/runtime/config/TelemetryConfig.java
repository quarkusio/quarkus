package io.quarkus.websockets.next.runtime.config;

import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configures telemetry in the WebSockets extension.
 */
public interface TelemetryConfig {

    /**
     * If collection of WebSocket traces is enabled.
     * Only applicable when the OpenTelemetry extension is present.
     */
    @WithName("traces.enabled")
    @WithDefault("true")
    boolean tracesEnabled();

    /**
     * If collection of WebSocket metrics is enabled.
     * Only applicable when the Micrometer extension is present.
     */
    @WithName("metrics.enabled")
    @WithDefault("false")
    boolean metricsEnabled();

}
