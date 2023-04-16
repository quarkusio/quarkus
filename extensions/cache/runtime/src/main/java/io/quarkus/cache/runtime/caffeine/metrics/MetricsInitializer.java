package io.quarkus.cache.runtime.caffeine.metrics;

import com.github.benmanes.caffeine.cache.AsyncCache;

import io.quarkus.cache.runtime.caffeine.CacheValue;

public interface MetricsInitializer {

    boolean metricsEnabled();

    void recordMetrics(AsyncCache<Object, CacheValue<Object>> cache, String cacheName);
}
