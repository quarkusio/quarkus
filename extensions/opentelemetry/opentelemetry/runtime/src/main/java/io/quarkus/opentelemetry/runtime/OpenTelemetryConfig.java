package io.quarkus.opentelemetry.runtime;

import java.util.List;

import io.quarkus.opentelemetry.runtime.tracing.TracerConfig;
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

    /**
     * Comma separated list of OpenTelemetry propagators which must be supported.
     * <p>
     * Valid values are {@code b3, b3multi, baggage, jaeger, ottrace, tracecontext, xray}.
     * <p>
     * Default value is {@code traceContext,baggage}.
     */
    @ConfigItem(defaultValue = "tracecontext,baggage")
    public List<String> propagators;

    /** Build / static runtime config for tracer */
    public TracerConfig tracer;
}
