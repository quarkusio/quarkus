package io.quarkus.smallrye.metrics.deployment;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigRoot
@ConfigMapping(prefix = "quarkus.smallrye-metrics")
public interface SmallRyeMetricsConfig {

    /**
     * The path to the metrics handler.
     * By default, this value will be resolved as a path relative to `${quarkus.http.non-application-root-path}`.
     * If the management interface is enabled, the value will be resolved as a path relative to
     * `${quarkus.management.root-path}`.
     */
    @WithDefault("metrics")
    String path();

    /**
     * Whether metrics published by Quarkus extensions should be enabled.
     */
    @WithName("extensions.enabled")
    @WithDefault("true")
    boolean extensionsEnabled();

    /**
     * Apply Micrometer compatibility mode, where instead of regular 'base' and 'vendor' metrics,
     * Quarkus exposes the same 'jvm' metrics that Micrometer does. Application metrics are unaffected by this mode.
     * The use case is to facilitate migration from Micrometer-based metrics, because original dashboards for JVM metrics
     * will continue working without having to rewrite them.
     */
    @WithName("micrometer.compatibility")
    @WithDefault("false")
    boolean micrometerCompatibility();

    /**
     * Whether detailed JAX-RS metrics should be enabled.
     * <p>
     * See <a href=
     * "https://github.com/eclipse/microprofile-metrics/blob/2.3.x/spec/src/main/asciidoc/required-metrics.adoc#optional-rest">MicroProfile
     * Metrics: Optional REST metrics</a>.
     */
    @WithName("jaxrs.enabled")
    @WithDefault("false")
    boolean jaxrsEnabled();
}