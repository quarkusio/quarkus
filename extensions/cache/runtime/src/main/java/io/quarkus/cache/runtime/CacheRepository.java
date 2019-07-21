package io.quarkus.cache.runtime;

import java.util.Collections;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.cache.runtime.caffeine.CaffeineCache;

@ApplicationScoped
public class CacheRepository {

    // There's no need for concurrency here since the map is created at build time and never modified after that.
    private Map<String, CaffeineCache> caches;

    public void setCaches(Map<String, CaffeineCache> caches) {
        if (this.caches != null) {
            throw new IllegalStateException("The caches map must only be set at build time");
        }
        this.caches = Collections.unmodifiableMap(caches);
    }

    public CaffeineCache getCache(String cacheName) {
        return caches.get(cacheName);
    }
}
