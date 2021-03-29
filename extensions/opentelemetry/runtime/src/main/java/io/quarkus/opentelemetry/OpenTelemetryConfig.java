package io.quarkus.opentelemetry;

import io.quarkus.opentelemetry.tracing.TracerConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "opentelemetry")
public final class OpenTelemetryConfig {

    /**
     * OpenTelemetry support.
     * <p>
     * OpenTelemetry support is enabled by default.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /** Build / static runtime config for tracer */
    public TracerConfig tracer;
}
