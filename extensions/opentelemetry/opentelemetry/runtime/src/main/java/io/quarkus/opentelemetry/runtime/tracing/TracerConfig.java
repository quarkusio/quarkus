package io.quarkus.opentelemetry.runtime.tracing;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class TracerConfig {
    /**
     * Support for tracing with OpenTelemetry.
     * <p>
     * Support for tracing will be enabled if OpenTelemetry support is enabled
     * and either this value is true, or this value is unset.
     */
    @ConfigItem
    public Optional<Boolean> enabled;

    /** Build / static runtime config for span exporters */
    public SpanExporterConfig exporter;

    /** Build / static runtime config for span exporters */
    @ConfigGroup
    public static class SpanExporterConfig {
    }
}
