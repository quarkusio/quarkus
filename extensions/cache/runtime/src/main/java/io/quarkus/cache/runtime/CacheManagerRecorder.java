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
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class CacheManagerRecorder {

    private final CacheBuildConfig cacheBuildConfig;
    private final RuntimeValue<CacheConfig> cacheConfigRV;

    public CacheManagerRecorder(CacheBuildConfig cacheBuildConfig, RuntimeValue<CacheConfig> cacheConfigRV) {
        this.cacheBuildConfig = cacheBuildConfig;
        this.cacheConfigRV = cacheConfigRV;
    }

    private CacheManagerInfo.Context createContextForCacheType(
            String cacheType,
            boolean micrometerMetricsEnabled) {
        return new CacheManagerInfo.Context() {
            private final Set<String> cacheNames = new HashSet<>();

            @Override
            public boolean cacheEnabled() {
                return cacheConfigRV.getValue().enabled();
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

    private Map<String, String> mapCacheTypeByCacheName(Set<String> cacheNames) {
        Map<String, String> cacheTypeByName = new HashMap<>(cacheNames.size());
        for (String cacheName : cacheNames) {
            CacheBuildConfig.CacheTypeBuildConfig cacheTypeBuildConfig = cacheBuildConfig.cacheTypeByName().get(cacheName);
            // check if the cache type is defined for this cache name "quarkus.cache.<cache-name>.type"
            if (cacheTypeBuildConfig != null &&
                    cacheTypeBuildConfig.type() != null &&
                    !cacheTypeBuildConfig.type().isEmpty()) {
                cacheTypeByName.put(cacheName, cacheTypeBuildConfig.type());
            } else {
                // if not, use the default cache type defined "quarkus.cache.type"
                cacheTypeByName.put(cacheName, cacheBuildConfig.type());
            }
        }
        return cacheTypeByName;
    }

    private Supplier<CacheManager> findSupplierForType(
            CacheManagerInfo.Context context,
            Collection<CacheManagerInfo> infos) {
        for (CacheManagerInfo info : infos) {
            if (info.supports(context)) {
                return info.get(context);
            }
        }
        throw new DeploymentException("Unknown cache type: " + context.cacheType());
    }

    private Map<String, Supplier<CacheManager>> createCacheSupplierByCacheType(
            Collection<CacheManagerInfo> infos,
            Set<String> cacheNames,
            boolean micrometerMetricsEnabled) {

        Map<String, String> cacheTypeByCacheName = mapCacheTypeByCacheName(cacheNames);

        // create one context per cache type with their corresponding list of cache names
        Map<String, CacheManagerInfo.Context> contextByCacheType = new HashMap<>();
        for (String cacheName : cacheNames) {
            contextByCacheType.computeIfAbsent(
                    cacheTypeByCacheName.get(cacheName),
                    cacheType -> this.createContextForCacheType(cacheType, micrometerMetricsEnabled))
                    .cacheNames()
                    .add(cacheName);
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

    public Supplier<CacheManager> resolveCacheInfo(
            Collection<CacheManagerInfo> infos, Set<String> cacheNames,
            boolean micrometerMetricsEnabled) {

        Map<String, Supplier<CacheManager>> suppliersByType = createCacheSupplierByCacheType(
                infos,
                cacheNames,
                micrometerMetricsEnabled);

        return new Supplier<CacheManager>() {
            @Override
            public CacheManager get() {
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
        };
    }

    public CacheManagerInfo noOpCacheManagerInfo() {
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

    public CacheManagerInfo getCacheManagerInfoWithMicrometerMetrics() {
        return new CacheManagerInfo() {
            @Override
            public boolean supports(Context context) {
                return context.cacheEnabled() && context.cacheType().equals(CAFFEINE_CACHE_TYPE)
                        && (context.metrics() == Context.Metrics.MICROMETER);
            }

            @Override
            public Supplier<CacheManager> get(Context context) {
                return CaffeineCacheManagerBuilder.buildWithMicrometerMetrics(context.cacheNames(), cacheConfigRV.getValue());
            }
        };
    }

    public CacheManagerInfo getCacheManagerInfoWithoutMetrics() {
        return new CacheManagerInfo() {
            @Override
            public boolean supports(Context context) {
                return context.cacheEnabled() && context.cacheType().equals(CAFFEINE_CACHE_TYPE)
                        && (context.metrics() == Context.Metrics.NONE);
            }

            @Override
            public Supplier<CacheManager> get(Context context) {
                return CaffeineCacheManagerBuilder.buildWithoutMetrics(context.cacheNames(), cacheConfigRV.getValue());
            }
        };
    }

}
