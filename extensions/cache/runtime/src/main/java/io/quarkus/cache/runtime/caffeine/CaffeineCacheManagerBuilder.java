package io.quarkus.cache.runtime.caffeine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.runtime.CacheConfig;
import io.quarkus.cache.runtime.CacheManagerImpl;
import io.quarkus.cache.runtime.caffeine.metrics.MetricsInitializer;
import io.quarkus.cache.runtime.caffeine.metrics.MicrometerMetricsInitializer;
import io.quarkus.cache.runtime.caffeine.metrics.NoOpMetricsInitializer;

public class CaffeineCacheManagerBuilder {

    private static final Logger LOGGER = Logger.getLogger(CaffeineCacheManagerBuilder.class);

    public static Supplier<CacheManager> buildWithMicrometerMetrics(Set<String> cacheNames, CacheConfig cacheConfig) {
        return build(cacheNames, cacheConfig, new MicrometerMetricsInitializer());
    }

    public static Supplier<CacheManager> buildWithoutMetrics(Set<String> cacheNames, CacheConfig cacheConfig) {
        return build(cacheNames, cacheConfig, new NoOpMetricsInitializer());
    }

    private static Supplier<CacheManager> build(Set<String> cacheNames, CacheConfig cacheConfig,
            MetricsInitializer metricsInitializer) {
        Set<CaffeineCacheInfo> cacheInfos = CaffeineCacheInfoBuilder.build(cacheNames, cacheConfig);
        return new Supplier<CacheManager>() {
            @Override
            public CacheManager get() {
                if (cacheInfos.isEmpty()) {
                    return new CacheManagerImpl(Collections.emptyMap());
                } else {
                    // The number of caches is known at build time so we can use fixed initialCapacity and loadFactor
                    // for the caches map.
                    Map<String, Cache> caches = new HashMap<>(cacheInfos.size() + 1, 1.0F);
                    for (CaffeineCacheInfo cacheInfo : cacheInfos) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debugf(
                                    "Building Caffeine cache [%s] with [initialCapacity=%s], [maximumSize=%s], [expireAfterWrite=%s], "
                                            + "[expireAfterAccess=%s] and [metricsEnabled=%s]",
                                    cacheInfo.name, cacheInfo.initialCapacity, cacheInfo.maximumSize,
                                    cacheInfo.expireAfterWrite, cacheInfo.expireAfterAccess, cacheInfo.metricsEnabled);
                        }
                        /*
                         * Metrics will be recorded for the current cache if: - the application depends on a
                         * quarkus-micrometer-registry-* extension - the metrics are enabled for this cache from the
                         * Quarkus configuration
                         */
                        boolean recordMetrics = metricsInitializer.metricsEnabled() && cacheInfo.metricsEnabled;
                        CaffeineCacheImpl cache = new CaffeineCacheImpl(cacheInfo, recordMetrics);
                        if (recordMetrics) {
                            metricsInitializer.recordMetrics(cache.cache, cacheInfo.name);
                        } else if (cacheInfo.metricsEnabled) {
                            LOGGER.warnf(
                                    "Metrics won't be recorded for cache '%s' because the application does not depend on a Micrometer extension. "
                                            + "This warning can be fixed by disabling the cache metrics in the configuration or by adding a Micrometer "
                                            + "extension to the pom.xml file.",
                                    cacheInfo.name);
                        }
                        caches.put(cacheInfo.name, cache);
                    }
                    return new CacheManagerImpl(caches);
                }
            }
        };
    }
}
