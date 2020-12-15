package io.quarkus.cache.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.runtime.caffeine.CaffeineCache;

public class CaffeineCacheSupplier implements Supplier<Collection<CaffeineCache>> {

    @Override
    public List<CaffeineCache> get() {
        CacheManager cacheManager = cacheManager();
        Collection<String> names = cacheManager.getCacheNames();
        List<CaffeineCache> allCaches = new ArrayList<>(names.size());
        for (String name : names) {
            Optional<Cache> cache = cacheManager.getCache(name);
            if (cache.isPresent() && cache.get() instanceof CaffeineCache) {
                allCaches.add((CaffeineCache) cache.get());
            }
        }
        allCaches.sort(Comparator.comparing(CaffeineCache::getName));
        return allCaches;
    }

    public static CacheManager cacheManager() {
        return Arc.container().instance(CacheManager.class).get();
    }
}
