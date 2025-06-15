package io.quarkus.cache.runtime.caffeine.metrics;

import org.jboss.logging.Logger;

import com.github.benmanes.caffeine.cache.AsyncCache;

/**
 * An instance of this class is created during the instantiation of the Caffeine caches when the application does not
 * depend on any quarkus-micrometer-registry-* extension. It is required to make the micrometer-core dependency
 * optional.
 */
public class NoOpMetricsInitializer implements MetricsInitializer {

    private static final Logger LOGGER = Logger.getLogger(NoOpMetricsInitializer.class);

    @Override
    public boolean metricsEnabled() {
        return false;
    }

    @Override
    public void recordMetrics(AsyncCache<Object, Object> cache, String cacheName) {
        LOGGER.tracef("Initializing no-op metrics for cache [%s]", cacheName);
        // Do nothing more.
    }
}
