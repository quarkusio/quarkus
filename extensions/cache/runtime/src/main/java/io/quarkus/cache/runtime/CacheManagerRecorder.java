package io.quarkus.cache.runtime;

import static io.quarkus.cache.runtime.CacheConfig.CAFFEINE_CACHE_TYPE;

import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.DeploymentException;

import io.quarkus.cache.CacheManager;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheManagerBuilder;
import io.quarkus.cache.runtime.noop.NoOpCacheManagerBuilder;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class CacheManagerRecorder {

    private final CacheConfig cacheConfig;

    public CacheManagerRecorder(CacheConfig cacheConfig) {
        this.cacheConfig = cacheConfig;
    }

    public Supplier<CacheManager> getCacheManagerSupplierWithMicrometerMetrics(Set<String> cacheNames) {
        Supplier<Supplier<CacheManager>> caffeineCacheManagerSupplier = new Supplier<Supplier<CacheManager>>() {
            @Override
            public Supplier<CacheManager> get() {
                return CaffeineCacheManagerBuilder.buildWithMicrometerMetrics(cacheNames, cacheConfig);
            }
        };
        return getCacheManagerSupplier(cacheNames, caffeineCacheManagerSupplier);
    }

    public Supplier<CacheManager> getCacheManagerSupplierWithoutMetrics(Set<String> cacheNames) {
        Supplier<Supplier<CacheManager>> caffeineCacheManagerSupplier = new Supplier<Supplier<CacheManager>>() {
            @Override
            public Supplier<CacheManager> get() {
                return CaffeineCacheManagerBuilder.buildWithoutMetrics(cacheNames, cacheConfig);
            }
        };
        return getCacheManagerSupplier(cacheNames, caffeineCacheManagerSupplier);
    }

    private Supplier<CacheManager> getCacheManagerSupplier(Set<String> cacheNames,
            Supplier<Supplier<CacheManager>> caffeineCacheManagerSupplier) {
        if (cacheConfig.enabled) {
            switch (cacheConfig.type) {
                case CAFFEINE_CACHE_TYPE:
                    return caffeineCacheManagerSupplier.get();
                default:
                    throw new DeploymentException("Unknown cache type: " + cacheConfig.type);
            }
        } else {
            return NoOpCacheManagerBuilder.build(cacheNames);
        }
    }
}
