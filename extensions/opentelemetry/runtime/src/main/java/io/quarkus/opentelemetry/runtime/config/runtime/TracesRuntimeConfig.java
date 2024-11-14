package io.quarkus.opentelemetry.runtime.config.runtime;

import java.util.List;
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
     * Comma-separated, suppress application uris from trace collection.
     * <p>
     * This will suppress all uris set by this property.
     * <p>
     * If you are using <code>quarkus.http.root-path</code>, you need to consider it when setting your uris, in
     * other words, you need to configure it using the root-path if necessary.
     */
    @WithName("suppress-application-uris")
    Optional<List<String>> suppressApplicationUris();

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
     * Sampler argument. Depends on the `quarkus.otel.traces.sampler` property.
     * Fallbacks to the legacy property <code>quarkus.opentelemetry.tracer.sampler.ratio</code>.
     * <p>
     * When setting the stock sampler to `traceidratio` or `parentbased_traceidratio` you need to set a `double` compatible
     * value between `0.0d` and `1.0d`, like `0.01d` or `0.5d`. It is kept as a `String` to allow the flexible customisation of
     * alternative samplers.
     * <p>
     * Defaults to `1.0d`.
     */
    @WithName("sampler.arg")
    @WithDefault("1.0d")
    Optional<String> samplerArg();
}
