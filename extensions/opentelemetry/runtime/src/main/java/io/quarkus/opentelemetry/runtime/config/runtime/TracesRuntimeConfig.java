package io.quarkus.opentelemetry.runtime.config.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class TracesRuntimeConfig {

    /**
     * Suppress non-application uris from trace collection.
     * This will suppress tracing of `/q` endpoints.
     * <p>
     * Providing a custom {@code io.opentelemetry.sdk.trace.samplers.Sampler} CDI Bean
     * will ignore this setting.
     * <p>
     * This is a Quarkus specific property.
     * The legacy property was: quarkus.opentelemetry.tracer.suppress-non-application-uris
     * <p>
     * Suppressing non-application uris is enabled by default.
     */
    @ConfigItem(name = "suppress-non-application-uris", defaultValue = "${opentelemetry.tracer.suppress-non-application-uris:true}")
    public Boolean suppressNonApplicationUris;

    /**
     * Include static resources from trace collection.
     * <p>
     * This is a Quarkus specific property.
     * Providing a custom {@code io.opentelemetry.sdk.trace.samplers.Sampler} CDI Bean
     * will ignore this setting.
     * <p>
     * Include static resources is disabled by default.
     */
    @ConfigItem(name = "include-static-resources", defaultValue = "${quarkus.opentelemetry.tracer.include-static-resources:false}")
    public Boolean includeStaticResources;

    /**
     * An argument to the configured tracer if supported, for example a ratio.
     * <p>
     * Default ratio is 1.0 or the legacy quarkus.opentelemetry.tracer.sampler.ratio property
     */
    @ConfigItem(name = "sampler.arg", defaultValue = "${quarkus.opentelemetry.tracer.sampler.ratio:1.0d}")
    public Optional<Double> samplerArg;
}
