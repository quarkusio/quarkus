package io.quarkus.cache.runtime;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;

/**
 * This class is registered as an @ApplicationScoped synthetic bean at build time.
 */
public class CacheManagerImpl implements CacheManager {

    private final Map<String, Cache> caches;
    private final Set<String> cacheNames;

    public CacheManagerImpl(Map<String, Cache> caches) {
        Objects.requireNonNull(caches);
        this.caches = Collections.unmodifiableMap(caches);
        cacheNames = Collections.unmodifiableSet(caches.keySet());
    }

    @Override
    public Set<String> getCacheNames() {
        return cacheNames;
    }

    @Override
    public Optional<Cache> getCache(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(caches.get(name));
    }
}
