package io.quarkus.opentelemetry.runtime.config.build;

import static io.quarkus.opentelemetry.runtime.config.build.ExporterType.Constants.CDI_VALUE;

import java.util.List;
import java.util.Optional;

import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Tracing build time configuration
 */
@ConfigGroup
public class TracesBuildConfig {

    /**
     * Enable tracing with OpenTelemetry.
     * <p>
     * This property is not available in the Open Telemetry SDK. It's Quarkus specific.
     * <p>
     * Support for tracing will be enabled if OpenTelemetry support is enabled
     * and either this value is true, or this value is unset.
     */
    @Deprecated
    @ConfigItem(defaultValue = "${quarkus.opentelemetry.tracer.enabled:true}")
    public Optional<Boolean> enabled;

    /**
     * List of exporters supported by Quarkus.
     * <p>
     * List of exporters to be used for tracing, separated by commas.
     * Has one of the values on {@link ExporterType} `otlp`, `cdi`, `none` or the full qualified name of a class implementing
     * {@link io.opentelemetry.sdk.trace.export.SpanExporter}
     * <p>
     * Default on Quarkus is {@value ExporterType.Constants#CDI_VALUE}.
     *
     * @return
     */
    @ConfigItem(defaultValue = CDI_VALUE)
    public List<String> exporter;

    /**
     * The sampler to use for tracing.
     * <p>
     * Has one of the values on {@link SamplerType} `always_on`, `always_off`, `traceidratio`, `parentbased_always_on`,
     * `parentbased_always_off`, `parentbased_traceidratio` or the Sampler SPI name. This will use the OTel SPI hooks for the
     * {@link io.opentelemetry.sdk.trace.samplers.Sampler} implementation set in the provider:
     * {@link io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider}.
     * <p>
     * Defaults to the legacy quarkus.opentelemetry.tracer.sampler.sampler.name property or
     * {@value SamplerType.Constants#PARENT_BASED_ALWAYS_ON}
     */
    @ConfigItem(defaultValue = "${quarkus.opentelemetry.tracer.sampler:" + SamplerType.Constants.PARENT_BASED_ALWAYS_ON + "}")
    //    @ConvertWith(LegacySamplerNameConverter.class)
    public String sampler;
}
