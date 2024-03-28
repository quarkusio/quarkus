package io.quarkus.opentelemetry.runtime.logging;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME, name = "log.handler.open-telemetry")
public class OpenTelemetryLogConfig {
    /**
     * Determine whether to enable the OpenTelemetry logging handler
     */
    @ConfigItem
    public boolean enabled;
}
