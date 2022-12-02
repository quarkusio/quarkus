package io.quarkus.deployment.metrics;

import io.quarkus.builder.item.SimpleBuildItem;

public final class MetricsCapabilityBuildItem extends SimpleBuildItem {
    @FunctionalInterface
    public interface MetricsCapability<String> {
        boolean isSupported(String value);
    }

    final String path;
    final MetricsCapability<String> metricsCapability;

    public MetricsCapabilityBuildItem(MetricsCapability<String> metricsCapability) {
        this(metricsCapability, null);
    }

    public MetricsCapabilityBuildItem(MetricsCapability<String> metricsCapability, String path) {
        this.metricsCapability = metricsCapability;
        this.path = path;
    }

    /**
     * Test for a known metrics system to allow selective initialization of metrics
     * based using a known API. Avoid using deployment module artifacts. Ensure that
     * metrics API dependencies remain optional / compile-time only.
     *
     * @return true if this factory supports the named metrics system. Arbitrary
     *         strings are allowed. Constants are present for a few.
     * @see io.quarkus.runtime.metrics.MetricsFactory#MICROMETER
     * @see io.quarkus.runtime.metrics.MetricsFactory#MP_METRICS
     */
    public boolean metricsSupported(String name) {
        return metricsCapability.isSupported(name);
    }

    /**
     * @return the configured Metrics Endpoint (if an endpoint is enabled) or null
     */
    public String metricsEndpoint() {
        return path;
    }
}
