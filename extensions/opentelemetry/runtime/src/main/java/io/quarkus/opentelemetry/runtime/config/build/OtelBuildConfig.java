package io.quarkus.opentelemetry.runtime.config.build;

import static io.quarkus.opentelemetry.runtime.config.build.PropagatorType.Constants.BAGGAGE;
import static io.quarkus.opentelemetry.runtime.config.build.PropagatorType.Constants.TRACE_CONTEXT;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Build Time configuration where all the attributes related with
 * classloading must live because of the native image needs
 */
@ConfigRoot(name = "otel", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public final class OtelBuildConfig {
    public static final String INSTRUMENTATION_NAME = "io.quarkus.opentelemetry";

    /**
     * If false, disable the OpenTelemetry usage at build time. All other Otel properties will
     * be ignored at runtime.
     * <p>
     * Will pick up value from legacy property quarkus.opentelemetry.enabled
     * <p>
     * Defaults to true.
     */
    @Deprecated // TODO only use runtime (soon)
    @ConfigItem(defaultValue = "${quarkus.opentelemetry.enabled:true}")
    public boolean enabled;

    /**
     * Trace exporter configurations
     */
    public TracesBuildConfig traces;

    /**
     * No Metrics exporter for now
     */
    @ConfigItem(name = "metrics.exporter", defaultValue = "none")
    public List<String> metricsExporter;

    /**
     * No Log exporter for now
     */
    @ConfigItem(name = "logs.exporter", defaultValue = "none")
    public List<String> logsExporter;

    /**
     * The propagators to be used. Use a comma-separated list for multiple propagators.
     * <p>
     * Has values from {@link PropagatorType} or the full qualified name of a class implementing
     * {@link io.opentelemetry.context.propagation.TextMapPropagator}
     * <p>
     * Default is {@value PropagatorType.Constants#TRACE_CONTEXT},{@value PropagatorType.Constants#BAGGAGE} (W3C).
     */
    @ConfigItem(defaultValue = TRACE_CONTEXT + "," + BAGGAGE)
    public List<String> propagators;
}
