package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;

public class CacheNamesTest {

    private static final String CACHE_NAME_1 = "test-cache-1";
    private static final String CACHE_NAME_2 = "test-cache-2";
    private static final String CACHE_NAME_3 = "test-cache-3";
    private static final String CACHE_NAME_4 = "test-cache-4";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(CachedService1.class, CachedService2.class));

    @Inject
    CacheManager cacheManager;

    @CacheName(CACHE_NAME_4)
    Cache cache;

    @Test
    public void testCacheNamesCollection() {
        /*
         * The main goal of this test is to check that a cache with an empty name is not instantiated at build time because of
         * the bindings with an empty `cacheName` parameter from the cache interceptors or because of the @CacheName annotation
         * in CacheProducer.
         */
        List<String> cacheNames = new ArrayList<>(cacheManager.getCacheNames());
        assertEquals(4, cacheNames.size());
        assertTrue(cacheNames.containsAll(Arrays.asList(CACHE_NAME_1, CACHE_NAME_2, CACHE_NAME_3, CACHE_NAME_4)));
    }

    @ApplicationScoped
    static class CachedService1 {

        @CacheResult(cacheName = CACHE_NAME_1)
        public String getValue(Object key) {
            return new String();
        }

        @CacheInvalidate(cacheName = CACHE_NAME_2)
        public void invalidate(Object key) {
        }
    }

    @Singleton
    static class CachedService2 {

        @CacheResult(cacheName = CACHE_NAME_1)
        public String getValue(@CacheKey Object key, BigDecimal notPartOfTheKey) {
            return new String();
        }

        @CacheInvalidateAll(cacheName = CACHE_NAME_3)
        public void invalidateAll() {
        }
    }
}
