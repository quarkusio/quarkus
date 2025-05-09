package io.quarkus.opentelemetry.runtime.config.build;

import static io.quarkus.opentelemetry.runtime.config.build.ExporterType.Constants.CDI_VALUE;

import java.util.List;
import java.util.Optional;

import io.smallrye.config.WithDefault;

public interface MetricsBuildConfig {

    /**
     * Enable metrics with OpenTelemetry.
     * <p>
     * This property is not available in the Open Telemetry SDK. It's Quarkus specific.
     * <p>
     * Support for OpenTelemetry Metrics will be enabled if this value is true.
     * The OpenTelemetry SDK ( {@link io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig#enabled()} )
     * is enabled by default and if disabled, OpenTelemetry Metrics will also be disabled.
     */
    @WithDefault("false")
    Optional<Boolean> enabled();

    /**
     * The Metrics exporter to use.
     */
    @WithDefault(CDI_VALUE)
    List<String> exporter();
}
