package io.quarkus.opentelemetry.runtime.tracing;

import java.util.List;
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

    /**
     * Comma separated list of resources that represents the entity that is
     * producing telemetry.
     * <p>
     * Valid values are {@code beanstalk, ec2, ecs, eks, host, lambda, os,
     * process, processruntime}.
     */
    @ConfigItem
    public Optional<List<String>> resources;

    /** Build / static runtime config for span exporters */
    @ConfigGroup
    public static class SpanExporterConfig {
    }
}
