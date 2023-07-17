package io.quarkus.opentelemetry.runtime.config.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigGroup
public interface TracesRuntimeConfig {

    /**
     * Suppress non-application uris from trace collection.
     * This will suppress tracing of `/q` endpoints.
     * <p>
     * Providing a custom {@code io.opentelemetry.sdk.trace.samplers.Sampler} CDI Bean
     * will ignore this setting.
     * <p>
     * This is a Quarkus specific property. Suppressing non-application uris is enabled by default.
     * <p>
     * Fallbacks to the legacy property <code>quarkus.opentelemetry.tracer.suppress-non-application-uris</code>
     * or defaults to `true`.
     */
    @WithName("suppress-non-application-uris")
    @WithDefault("true")
    Boolean suppressNonApplicationUris();

    /**
     * Include static resources from trace collection.
     * <p>
     * This is a Quarkus specific property. Include static resources is disabled by default. Providing a
     * custom {@code io.opentelemetry.sdk.trace.samplers.Sampler} CDI Bean will ignore this setting.
     * <p>
     * Fallbacks to the legacy property <code>quarkus.opentelemetry.tracer.include-static-resources</code>
     * or defaults to `false`.
     */
    @WithName("include-static-resources")
    @WithDefault("false")
    Boolean includeStaticResources();

    /**
     * An argument to the configured tracer if supported, for example a ratio.
     * <p>
     * Fallbacks to the legacy property <code>quarkus.opentelemetry.tracer.sampler.ratio</code>
     * or defaults to `1.0`.
     */
    @WithName("sampler.arg")
    @WithDefault("1.0d")
    Optional<Double> samplerArg();
}
