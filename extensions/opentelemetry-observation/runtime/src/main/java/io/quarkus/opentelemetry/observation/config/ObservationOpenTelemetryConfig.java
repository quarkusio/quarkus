package io.quarkus.opentelemetry.observation.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.otel.observation")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface ObservationOpenTelemetryConfig {

    /**
     * Whether the OpenTelemetry Observation bridge is enabled.
     */
    @WithDefault("true")
    boolean enabled();
}
