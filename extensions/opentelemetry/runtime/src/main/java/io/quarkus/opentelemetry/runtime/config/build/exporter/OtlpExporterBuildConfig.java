package io.quarkus.opentelemetry.runtime.config.build.exporter;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.otel.exporter.otlp")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface OtlpExporterBuildConfig {
    /**
     * Will disable the Quarkus managed OpenTelemetry exporters. No telemetry will be sent output if set to `false`.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Experimental OTLP exporter options that may change or be removed in a future version.
     */
    Experimental experimental();

    interface Experimental {
        /**
         * If `true`, the built-in OTLP exporter is created even when another exporter
         * (a CDI bean or a Quarkiverse extension exporter) is already present for a signal, so both
         * export the same telemetry. This applies to all signals (traces, metrics and logs).
         * <p>
         * Setting a signal `exporter` to `none` still disables the built-in exporter for that signal,
         * regardless of this option.
         * <p>
         * This is a build time property. Defaults to `false`.
         */
        @WithDefault("false")
        boolean defaultEnabled();
    }
}
