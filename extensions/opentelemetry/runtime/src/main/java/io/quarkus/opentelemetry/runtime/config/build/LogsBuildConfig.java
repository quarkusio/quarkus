package io.quarkus.opentelemetry.runtime.config.build;

import static io.quarkus.opentelemetry.runtime.config.build.ExporterType.Constants.CDI_VALUE;

import java.util.List;
import java.util.Optional;

import io.smallrye.config.WithDefault;

public interface LogsBuildConfig {
    /**
     * Enable logs with OpenTelemetry.
     * <p>
     * This property is not available in the Open Telemetry SDK. It's Quarkus specific.
     * <p>
     * Support for logs will be enabled if OpenTelemetry support is enabled
     * and either this value is true, or this value is unset.
     */
    @WithDefault("false")
    Optional<Boolean> enabled();

    /**
     * The Logs exporter to use.
     */
    @WithDefault(CDI_VALUE)
    List<String> exporter();
}
