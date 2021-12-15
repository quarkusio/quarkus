package io.quarkus.cache.runtime.caffeine.metrics;

import com.github.benmanes.caffeine.cache.AsyncCache;

/**
 * An instance of this class is created during the instantiation of the Caffeine caches when the application does not depend on
 * any quarkus-micrometer-registry-* extension. It is required to make the micrometer-core dependency optional.
 */
public class NoOpMetricsInitializer implements MetricsInitializer {

    @Override
    public boolean metricsEnabled() {
        return false;
    }

    @Override
    public void recordMetrics(AsyncCache<Object, Object> cache, String cacheName) {
        // Do nothing.
    }
}
