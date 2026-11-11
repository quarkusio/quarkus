package io.quarkus.cache.runtime;

import static io.quarkus.cache.runtime.CacheBuildConfig.CAFFEINE_CACHE_TYPE;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.DeploymentException;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CacheManagerInfo;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheManagerBuilder;
import io.quarkus.cache.runtime.noop.NoOpCacheManagerBuilder;

public class CacheManagerRecorder {

    private static CacheManagerInfo.Context createContextForCacheType(
            String cacheType,
            boolean cacheEnabled,
            boolean micrometerMetricsEnabled) {
        return new CacheManagerInfo.Context() {
            private final Set<String> cacheNames = new HashSet<>();

            @Override
            public boolean cacheEnabled() {
                return cacheEnabled;
            }

            @Override
            public Metrics metrics() {
                return micrometerMetricsEnabled ? Metrics.MICROMETER : Metrics.NONE;
            }

            @Override
            public String cacheType() {
                return cacheType;
            }

            @Override
            public Set<String> cacheNames() {
                return cacheNames;
            }
        };
    }

    private static Supplier<CacheManager> findSupplierForType(
            CacheManagerInfo.Context context,
            Collection<CacheManagerInfo> infos) {
        for (CacheManagerInfo info : infos) {
            if (info.supports(context)) {
                return info.get(context);
            }
        }
        throw new DeploymentException("Unknown cache type: " + context.cacheType());
    }

    private static Map<String, Supplier<CacheManager>> createCacheSupplierByCacheType(
            Collection<CacheManagerInfo> infos,
            Map<String, String> cacheTypeByCacheName,
            boolean cacheEnabled,
            boolean micrometerMetricsEnabled) {

        // create one context per cache type with their corresponding list of cache names
        Map<String, CacheManagerInfo.Context> contextByCacheType = new HashMap<>();
        for (Map.Entry<String, String> entry : cacheTypeByCacheName.entrySet()) {
            contextByCacheType.computeIfAbsent(
                    entry.getValue(),
                    cacheType -> createContextForCacheType(cacheType, cacheEnabled, micrometerMetricsEnabled))
                    .cacheNames()
                    .add(entry.getKey());
        }

        // suppliers grouped by cache type
        Map<String, Supplier<CacheManager>> suppliersByType = new HashMap<>();
        for (Map.Entry<String, CacheManagerInfo.Context> entry : contextByCacheType.entrySet()) {
            String cacheType = entry.getKey();
            if (!suppliersByType.containsKey(cacheType)) {
                suppliersByType.put(cacheType, findSupplierForType(entry.getValue(), infos));
            }
        }

        return suppliersByType;
    }

    public static CacheManager createCacheManager(
            Collection<CacheManagerInfo> infos, Map<String, String> cacheTypeByCacheName,
            CacheConfig cacheConfig, boolean micrometerMetricsEnabled) {

        Map<String, Supplier<CacheManager>> suppliersByType = createCacheSupplierByCacheType(
                infos,
                cacheTypeByCacheName,
                cacheConfig.enabled(),
                micrometerMetricsEnabled);

        if (suppliersByType.size() == 1) {
            // if there is only one cache type, return the corresponding cache implementation
            return suppliersByType.values().iterator().next().get();
        }
        // if there are multiple cache types, return a CacheManager implementation that aggregates all caches

        // get the cache manager implementation by cache type of each supplier
        Map<String, CacheManager> cacheImplByCacheType = new HashMap<>();
        for (Map.Entry<String, Supplier<CacheManager>> entry : suppliersByType.entrySet()) {
            cacheImplByCacheType.put(entry.getKey(), entry.getValue().get());
        }

        // put all cache implementations together in a single map indexed by cache name
        Map<String, Cache> allCaches = new HashMap<>();
        for (CacheManager cacheManager : cacheImplByCacheType.values()) {
            for (String cacheName : cacheManager.getCacheNames()) {
                cacheManager.getCache(cacheName).ifPresent(cache -> allCaches.put(cacheName, cache));
            }
        }
        return new CacheManagerImpl(allCaches);
    }

    public static CacheManagerInfo noOpCacheManagerInfo() {
        return new CacheManagerInfo() {
            @Override
            public boolean supports(Context context) {
                return !context.cacheEnabled();
            }

            @Override
            public Supplier<CacheManager> get(Context context) {
                return NoOpCacheManagerBuilder.build(context.cacheNames());
            }
        };
    }

    public static CacheManagerInfo getCacheManagerInfoWithMicrometerMetrics(CacheConfig cacheConfig) {
        return new CacheManagerInfo() {
            @Override
            public boolean supports(Context context) {
                return context.cacheEnabled() && context.cacheType().equals(CAFFEINE_CACHE_TYPE)
                        && (context.metrics() == Context.Metrics.MICROMETER);
            }

            @Override
            public Supplier<CacheManager> get(Context context) {
                return CaffeineCacheManagerBuilder.buildWithMicrometerMetrics(context.cacheNames(), cacheConfig);
            }
        };
    }

    public static CacheManagerInfo getCacheManagerInfoWithoutMetrics(CacheConfig cacheConfig) {
        return new CacheManagerInfo() {
            @Override
            public boolean supports(Context context) {
                return context.cacheEnabled() && context.cacheType().equals(CAFFEINE_CACHE_TYPE)
                        && (context.metrics() == Context.Metrics.NONE);
            }

            @Override
            public Supplier<CacheManager> get(Context context) {
                return CaffeineCacheManagerBuilder.buildWithoutMetrics(context.cacheNames(), cacheConfig);
            }
        };
    }

}
