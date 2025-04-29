package io.quarkus.opentelemetry.runtime.config.build;

import static io.quarkus.opentelemetry.runtime.config.build.ExporterType.Constants.CDI_VALUE;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Tracing build time configuration
 */
@ConfigGroup
public interface TracesBuildConfig {

    /**
     * Enable tracing with OpenTelemetry.
     * <p>
     * This property is not available in the Open Telemetry SDK. It's Quarkus specific.
     * <p>
     * Support for OpenTelemetry Tracing will be enabled if this value is true.
     * The OpenTelemetry SDK ( {@link io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig#enabled()} )
     * is enabled by default and if disabled, OpenTelemetry Tracing will also be disabled.
     */
    @Deprecated
    @WithDefault("true")
    Optional<Boolean> enabled();

    /**
     * List of exporters supported by Quarkus.
     * <p>
     * List of exporters to be used for tracing, separated by commas.
     * Has one of the values on {@link ExporterType} `otlp`, `cdi`, `none` or the full qualified name of a class
     * implementing {@link io.opentelemetry.sdk.trace.export.SpanExporter}
     * <p>
     * Default on Quarkus is {@value ExporterType.Constants#CDI_VALUE}.
     */
    @WithDefault(CDI_VALUE)
    List<String> exporter();

    /**
     * The sampler to use for tracing.
     * <p>
     * Has one of the values on {@link SamplerType} `always_on`, `always_off`, `traceidratio`, `parentbased_always_on`,
     * `parentbased_always_off`, `parentbased_traceidratio` or the Sampler SPI name. This will use the OTel SPI hooks
     * for the {@link io.opentelemetry.sdk.trace.samplers.Sampler} implementation set in the provider:
     * {@link io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider}.
     * <p>
     * Fallbacks to the legacy property <code>quarkus.opentelemetry.tracer.sampler.sampler.name</code> or
     * defaults to {@value SamplerType.Constants#PARENT_BASED_ALWAYS_ON}.
     */
    @WithDefault(SamplerType.Constants.PARENT_BASED_ALWAYS_ON)
    String sampler();

    /**
     * If OpenTelemetry End User attributes should be added as Span attributes on a best-efforts basis.
     *
     * @see <a href="https://opentelemetry.io/docs/specs/semconv/attributes-registry/enduser/">OpenTelemetry End User
     *      attributes</a>
     */
    @WithName("eusp.enabled")
    @WithDefault("false")
    boolean addEndUserAttributes();

}
