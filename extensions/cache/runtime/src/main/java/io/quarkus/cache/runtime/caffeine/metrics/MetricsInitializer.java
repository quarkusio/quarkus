package io.quarkus.cache.runtime.caffeine.metrics;

import com.github.benmanes.caffeine.cache.AsyncCache;

public interface MetricsInitializer {

    boolean metricsEnabled();

    void recordMetrics(AsyncCache<Object, Object> cache, String cacheName);
}
