package io.quarkus.cache.runtime.caffeine.metrics;

import org.jboss.logging.Logger;

import com.github.benmanes.caffeine.cache.AsyncCache;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;

/**
 * An instance of this class is created during the instantiation of the Caffeine caches when the application depends on
 * a quarkus-micrometer-registry-* extension.
 */
public class MicrometerMetricsInitializer implements MetricsInitializer {

    private static final Logger LOGGER = Logger.getLogger(MicrometerMetricsInitializer.class);

    @Override
    public boolean metricsEnabled() {
        return true;
    }

    @Override
    public void recordMetrics(AsyncCache<Object, Object> cache, String cacheName) {
        LOGGER.tracef("Initializing Micrometer metrics for cache [%s]", cacheName);
        // The 'tags' vararg is purposely empty here. Tags should be configured using MeterFilter.
        CaffeineCacheMetrics.monitor(Metrics.globalRegistry, cache, cacheName);
    }
}
