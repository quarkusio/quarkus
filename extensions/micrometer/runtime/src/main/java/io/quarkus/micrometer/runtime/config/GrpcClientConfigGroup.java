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
     * When enabled, aggregatable latency buckets are published (suitable for
     * {@code histogram_quantile} in Prometheus).
     */
    @WithDefault("false")
    boolean histogram();

    /**
     * Service level objective (bucket) boundaries for the processing duration histogram.
     * <p>
     * Only applied when {@link #histogram()} is {@code true}. Using a fixed set of buckets
     * keeps metric cardinality bounded compared to Micrometer's full percentile histogram
     * generator.
     */
    @WithDefault("5ms,10ms,25ms,50ms,100ms,250ms,500ms,1s,5s")
    List<Duration> slos();
}
