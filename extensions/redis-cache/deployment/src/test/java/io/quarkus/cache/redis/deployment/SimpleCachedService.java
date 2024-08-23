package io.quarkus.cache.redis.deployment;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;

@ApplicationScoped
public class SimpleCachedService {

    static final String CACHE_NAME = "test-cache";

    @CacheResult(cacheName = CACHE_NAME)
    public String cachedMethod(String key) {
        return UUID.randomUUID().toString();
    }

    @CacheInvalidate(cacheName = CACHE_NAME)
    public void invalidate(String key) {
    }

    @CacheInvalidateAll(cacheName = CACHE_NAME)
    public void invalidateAll() {
    }
}
