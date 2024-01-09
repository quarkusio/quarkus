package io.quarkus.opentelemetry.runtime.config.build;

import static io.quarkus.opentelemetry.runtime.config.build.PropagatorType.Constants.BAGGAGE;
import static io.quarkus.opentelemetry.runtime.config.build.PropagatorType.Constants.TRACE_CONTEXT;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Build Time configuration where all the attributes related with
 * classloading must live because of the native image needs
 */
@ConfigMapping(prefix = "quarkus.otel")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface OTelBuildConfig {

    String INSTRUMENTATION_NAME = "io.quarkus.opentelemetry";

    /**
     * If false, disable the OpenTelemetry usage at build time. All other Otel properties will
     * be ignored at runtime.
     * <p>
     * Will pick up value from legacy property quarkus.opentelemetry.enabled
     * <p>
     * Defaults to <code>true</code>.
     */
    @Deprecated // TODO only use runtime (soon)
    @WithDefault("true")
    boolean enabled();

    /**
     * Trace exporter configurations.
     */
    TracesBuildConfig traces();

    /**
     * No Metrics exporter for now
     */
    @WithName("metrics.exporter")
    @WithDefault("none")
    List<String> metricsExporter();

    /**
     * No Log exporter for now.
     */
    @WithName("logs.exporter")
    @WithDefault("none")
    List<String> logsExporter();

    /**
     * The propagators to be used. Use a comma-separated list for multiple propagators.
     * <p>
     * Has values from {@link PropagatorType} or the full qualified name of a class implementing
     * {@link io.opentelemetry.context.propagation.TextMapPropagator}.
     * <p>
     * Default is {@value PropagatorType.Constants#TRACE_CONTEXT},{@value PropagatorType.Constants#BAGGAGE} (W3C).
     */
    @WithDefault(TRACE_CONTEXT + "," + BAGGAGE)
    List<String> propagators();

    /**
     * Enable/disable instrumentation for specific technologies.
     */
    InstrumentBuildTimeConfig instrument();
}
