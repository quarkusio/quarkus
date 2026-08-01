package io.quarkus.micrometer.runtime.config;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * Build / static runtime config for gRPC Client.
 */
@ConfigGroup
public interface GrpcClientConfigGroup extends MicrometerConfig.CapabilityEnabled {
    /**
     * gRPC Client metrics support.
     * <p>
     * Support for gRPC client metrics will be enabled if Micrometer support is enabled,
     * the gRPC client interfaces are on the classpath
     * and either this value is true, or this value is unset and
     * {@code quarkus.micrometer.binder-enabled-default} is true.
     */
    @Override
    Optional<Boolean> enabled();

    /**
     * Whether to publish histogram buckets for gRPC client processing duration timers.
     * <p>
     * Disabled by default because histograms increase memory usage and metric cardinality.
     * When enabled, Micrometer's default percentile histogram buckets are published (suitable
     * for {@code histogram_quantile} in Prometheus). Optional SLO boundaries can be added via
     * {@link #slos()}.
     */
    @WithDefault("false")
    boolean histogram();

    /**
     * Optional service level objective (bucket) boundaries for the processing duration histogram.
     * <p>
     * Only applied when {@link #histogram()} is {@code true}. When unset, Micrometer's default
     * percentile histogram buckets are used. When set, these SLO boundaries are added to that
     * histogram.
     */
    Optional<List<Duration>> slos();
}
