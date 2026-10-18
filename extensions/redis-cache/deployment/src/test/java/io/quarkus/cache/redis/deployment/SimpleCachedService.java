package io.quarkus.cache.redis.deployment;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class SimpleCachedService {

    static final String CACHE_NAME = "test-cache";
    static final String FAILING_CACHE_NAME = "failing-cache";

    @CacheResult(cacheName = CACHE_NAME)
    public String cachedMethod(String key) {
        return UUID.randomUUID().toString();
    }

    @CacheResult(cacheName = FAILING_CACHE_NAME)
    public Uni<String> failingCachedMethod(String key) {
        return Uni.createFrom().failure(new IllegalStateException("value loader failed"));
    }

    @CacheInvalidate(cacheName = CACHE_NAME)
    public void invalidate(String key) {
    }

    @CacheInvalidateAll(cacheName = CACHE_NAME)
    public void invalidateAll() {
    }
}
