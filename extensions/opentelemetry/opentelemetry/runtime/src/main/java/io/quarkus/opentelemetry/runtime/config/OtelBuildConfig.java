package io.quarkus.opentelemetry.runtime.config;

import static io.quarkus.opentelemetry.runtime.config.PropagatorType.Constants.BAGGAGE;
import static io.quarkus.opentelemetry.runtime.config.PropagatorType.Constants.TRACE_CONTEXT;

import java.util.List;

import io.quarkus.opentelemetry.runtime.tracing.config.TracesBuildConfig;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "otel")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface OtelBuildConfig {

    String INSTRUMENTATION_NAME = "io.quarkus.opentelemetry";

    /**
     * If false, disable the OpenTelemetry usage at build time. All other Otel properties will
     * be ignored at runtime.
     * <p>
     * Will pick up value from legacy property quarkus.opentelemetry.enabled
     * <p>
     * Defaults to true.
     */
    @Deprecated // TODO only use runtime
    @WithDefault("${quarkus.opentelemetry.enabled:true}") // FIXME use fallback
    Boolean enabled();

    /**
     * Trace exporter configurations
     */
    TracesBuildConfig traces();

    /**
     * No Metrics exporter for now
     */
    @WithDefault("none")
    @WithName("metrics.exporter")
    List<String> metricsExporter();

    /**
     * No Log exporter for now
     */
    @WithDefault("none")
    @WithName("logs.exporter")
    List<String> logsExporter();

    /**
     * The propagators to be used. Use a comma-separated list for multiple propagators.
     * <p>
     * Has values from {@link PropagatorType} or the full qualified name of a class implementing
     * {@link io.opentelemetry.context.propagation.TextMapPropagator}
     * <p>
     * Default is {@value PropagatorType.Constants#TRACE_CONTEXT},{@value PropagatorType.Constants#BAGGAGE} (W3C).
     */
    @WithDefault(TRACE_CONTEXT + "," + BAGGAGE)
    List<String> propagators();
}
