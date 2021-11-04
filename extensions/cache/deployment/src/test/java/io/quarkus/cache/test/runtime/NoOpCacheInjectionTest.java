package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.runtime.noop.NoOpCache;
import io.quarkus.test.QuarkusUnitTest;

public class NoOpCacheInjectionTest {

    private static final String CACHE_NAME = "test-cache";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addAsResource(new StringAsset("quarkus.cache.enabled=false"), "application.properties")
            .addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @CacheName(CACHE_NAME)
    Cache cache;

    @Inject
    CacheManager cacheManager;

    @Test
    public void testInjection() {
        assertEquals(NoOpCache.class, cache.getClass());
        assertEquals(cache, cacheManager.getCache(CACHE_NAME).get());
    }

    @Singleton
    static class CachedService {

        @CacheInvalidateAll(cacheName = CACHE_NAME)
        public void invalidateAll() {
        }
    }
}
