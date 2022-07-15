package io.quarkus.opentelemetry.runtime.tracing.config;

import static io.quarkus.opentelemetry.runtime.config.ExporterType.Constants.CDI_VALUE;
import static io.quarkus.opentelemetry.runtime.tracing.config.TracesBuildConfig.SamplerType.Constants.*;

import java.util.List;
import java.util.Optional;

import io.quarkus.opentelemetry.runtime.config.ExporterType;
import io.quarkus.opentelemetry.runtime.tracing.config.TracesBuildConfig.SamplerType.Constants;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

public interface TracesBuildConfig {

    /**
     * Enable tracing with OpenTelemetry.
     * <p>
     * This property is not available in the Open Telemetry SDK. It's Quarkus specific.
     * <p>
     * Support for tracing will be enabled if OpenTelemetry support is enabled
     * and either this value is true, or this value is unset.
     */
    @Deprecated()
    @WithDefault("${quarkus.opentelemetry.tracer.enabled:true}")
    Optional<Boolean> enabled();

    /**
     * List of exporters supported by Quarkus.
     * <p>
     * List of exporters to be used for tracing, separated by commas. none means no autoconfigured exporter.
     * Has one of the values on {@link ExporterType} or the full qualified name of a class implementing
     * {@link io.opentelemetry.sdk.trace.export.SpanExporter}
     * <p>
     * Default is {@value ExporterType.Constants#CDI_VALUE}.
     *
     * @return
     */
    @WithDefault(CDI_VALUE)
    List<String> exporter();

    /**
     * Trace sampler configs
     */
    SamplerConfig sampler();

    /**
     * Quarkus specific extra properties
     */
    interface SamplerConfig {
        /**
         * The sampler to use for tracing.
         * <p>
         * Has one of the values on {@link SamplerType} or the full qualified name of a class implementing
         * {@link io.opentelemetry.sdk.trace.samplers.Sampler}
         * <p>
         * Defaults to {@value Constants#PARENT_BASED_ALWAYS_ON} or
         * the legacy quarkus.opentelemetry.tracer.sampler.sampler.name property
         */
        @WithDefault("${quarkus.opentelemetry.tracer.sampler:" + PARENT_BASED_ALWAYS_ON + "}")
        @WithParentName
        @WithConverter(LegacySamplerNameConverter.class)
        String sampler();

        /**
         * An argument to the configured tracer if supported, for example a ratio.
         * <p>
         * Default ratio is 1.0 or the legacy quarkus.opentelemetry.tracer.sampler.ratio property
         */
        @WithDefault("${quarkus.opentelemetry.tracer.sampler.ratio:1.0d}")
        Optional<Double> arg();
    }

    enum SamplerType {
        ALWAYS_ON(Constants.ALWAYS_ON),
        ALWAYS_OFF(Constants.ALWAYS_OFF),
        TRACE_ID_RATIO(Constants.TRACE_ID_RATIO),
        PARENT_BASED_ALWAYS_ON(Constants.PARENT_BASED_ALWAYS_ON),
        PARENT_BASED_ALWAYS_OFF(Constants.PARENT_BASED_ALWAYS_OFF),
        PARENT_BASED_TRACE_ID_RATIO(Constants.PARENT_BASED_TRACE_ID_RATIO);

        private String value;

        SamplerType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        static class Constants {
            public static final String ALWAYS_ON = "always_on";
            public static final String ALWAYS_OFF = "always_off";
            public static final String TRACE_ID_RATIO = "traceidratio";
            public static final String PARENT_BASED_ALWAYS_ON = "parentbased_always_on";
            public static final String PARENT_BASED_ALWAYS_OFF = "parentbased_always_off";
            public static final String PARENT_BASED_TRACE_ID_RATIO = "parentbased_traceidratio";
        }
    }
}
