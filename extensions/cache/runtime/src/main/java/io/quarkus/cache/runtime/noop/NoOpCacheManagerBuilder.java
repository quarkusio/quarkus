package io.quarkus.cache.runtime.noop;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.runtime.CacheManagerImpl;

public class NoOpCacheManagerBuilder {

    public static Supplier<CacheManager> build(Set<String> cacheNames) {
        Objects.requireNonNull(cacheNames);
        return new Supplier<CacheManager>() {
            @Override
            public CacheManager get() {
                if (cacheNames.isEmpty()) {
                    return new CacheManagerImpl(Collections.emptyMap());
                } else {
                    // The number of caches is known at build time so we can use fixed initialCapacity and loadFactor for the caches map.
                    Map<String, Cache> caches = new HashMap<>(cacheNames.size() + 1, 1.0F);
                    NoOpCache cache = new NoOpCache();
                    for (String cacheName : cacheNames) {
                        caches.put(cacheName, cache);
                    }
                    return new CacheManagerImpl(caches);
                }
            }
        };
    }
}
