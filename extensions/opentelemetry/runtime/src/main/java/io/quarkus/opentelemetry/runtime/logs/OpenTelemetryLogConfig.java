package io.quarkus.opentelemetry.runtime.logs;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.log.handler.open-telemetry")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface OpenTelemetryLogConfig {
    /**
     * Determine whether to enable the OpenTelemetry logging handler
     */
    @WithDefault("false")
    boolean enabled();
}
