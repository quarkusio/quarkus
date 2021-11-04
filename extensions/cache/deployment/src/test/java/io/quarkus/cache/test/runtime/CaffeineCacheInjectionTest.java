package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.runtime.caffeine.CaffeineCache;
import io.quarkus.test.QuarkusUnitTest;

public class CaffeineCacheInjectionTest {

    private static final String CACHE_NAME = "test-cache";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @CacheName(CACHE_NAME)
    Cache cache;

    @Inject
    CacheManager cacheManager;

    @Test
    public void testInjection() {
        assertEquals(CaffeineCache.class, cache.getClass());
        assertEquals(cache, cacheManager.getCache(CACHE_NAME).get());
        assertEquals(cache, cachedService.getConstructorInjectedCache());
        assertEquals(cache, cachedService.getMethodInjectedCache());
    }

    @ApplicationScoped
    static class CachedService {

        Cache constructorInjectedCache;
        Cache methodInjectedCache;

        public CachedService(@CacheName(CACHE_NAME) Cache cache) {
            constructorInjectedCache = cache;
        }

        public Cache getConstructorInjectedCache() {
            return constructorInjectedCache;
        }

        public Cache getMethodInjectedCache() {
            return methodInjectedCache;
        }

        @Inject
        public void setMethodInjectedCache(@CacheName(CACHE_NAME) Cache cache) {
            methodInjectedCache = cache;
        }

        @CacheInvalidateAll(cacheName = CACHE_NAME)
        public void invalidateAll() {
        }
    }
}
