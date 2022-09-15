package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheImpl;
import io.quarkus.test.QuarkusUnitTest;

public class ProgrammaticApiTest {

    private static final String CACHE_NAME_1 = "test-cache-1";
    private static final String CACHE_NAME_2 = "test-cache-2";
    private static final Object KEY_1 = new Object();
    private static final Object KEY_2 = new Object();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot(jar -> jar.addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Inject
    CacheManager cacheManager;

    @CacheName(CACHE_NAME_1)
    Cache cache;

    @CacheName(CACHE_NAME_2)
    Cache anotherCache;

    @Test
    public void testInjection() {
        assertTrue(cacheManager.getCacheNames().containsAll(List.of(CACHE_NAME_1, CACHE_NAME_2)));
        assertSame(CaffeineCacheImpl.class, cache.getClass());
        assertSame(CaffeineCacheImpl.class, anotherCache.getClass());
        assertSame(cache, cacheManager.getCache(CACHE_NAME_1).get());
        assertSame(cache, cachedService.getConstructorInjectedCache());
        assertSame(cache, cachedService.getMethodInjectedCache());
        assertNotSame(cache, anotherCache);
    }

    @Test
    public void testAllCacheAnnotationsAndMethods() throws Exception {
        assertKeySetContains();
        assertGetIfPresentMissingKey(KEY_1);
        assertGetIfPresentMissingKey(KEY_2);

        // STEP 1
        // Action: @CacheResult-annotated method call.
        // Expected effect: method invoked and result cached.
        // Verified by: STEP 2.
        String value1 = cachedService.cachedMethod(KEY_1);
        assertKeySetContains(KEY_1);

        // STEP 2
        // Action: value retrieval from the cache with the same key as STEP 1.
        // Expected effect: value loader function not invoked.
        // Verified by: same object reference between STEPS 1 and 2 results.
        String value2 = cache.get(KEY_1, k -> new String()).await().indefinitely();
        assertSame(value1, value2);
        assertKeySetContains(KEY_1);
        assertGetIfPresent(KEY_1, value2);
        assertGetIfPresentMissingKey(KEY_2);

        // STEP 3
        // Action: value retrieval from the cache with a new key.
        // Expected effect: value loader function invoked and result cached.
        // Verified by: STEP 4.
        String value3 = cache.get(KEY_2, k -> new String()).await().indefinitely();
        assertNotSame(value2, value3);
        assertKeySetContains(KEY_1, KEY_2);

        // STEP 4
        // Action: @CacheResult-annotated method call with the same key as STEP 3.
        // Expected effect: method not invoked and result coming from the cache.
        // Verified by: same object reference between STEPS 3 and 4 results.
        String value4 = cachedService.cachedMethod(KEY_2);
        assertSame(value3, value4);
        assertKeySetContains(KEY_1, KEY_2);
        assertGetIfPresent(KEY_1, value2);
        assertGetIfPresent(KEY_2, value4);

        // STEP 5
        // Action: cache entry invalidation.
        // Expected effect: STEP 2 cache entry removed.
        // Verified by: STEP 6.
        cache.invalidate(KEY_1).await().indefinitely();
        assertKeySetContains(KEY_2);
        assertGetIfPresentMissingKey(KEY_1);
        assertGetIfPresent(KEY_2, value4);

        // STEP 6
        // Action: value retrieval from the cache with the same key as STEP 2.
        // Expected effect: value loader function invoked because of STEP 5 and result cached.
        // Verified by: different objects references between STEPS 2 and 6 results.
        String value6 = cache.get(KEY_1, k -> new String()).await().indefinitely();
        assertNotSame(value2, value6);
        assertKeySetContains(KEY_1, KEY_2);
        assertGetIfPresent(KEY_1, value6);
        assertGetIfPresent(KEY_2, value4);

        // STEP 7
        // Action: value retrieval from the cache with the same key as STEP 4.
        // Expected effect: value loader function not invoked.
        // Verified by: same object reference between STEPS 4 and 7 results.
        String value7 = cache.get(KEY_2, k -> new String()).await().indefinitely();
        assertSame(value4, value7);
        assertKeySetContains(KEY_1, KEY_2);
        assertGetIfPresent(KEY_1, value6);
        assertGetIfPresent(KEY_2, value4);

        // STEP 8
        // Action: full cache invalidation.
        // Expected effect: empty cache.
        // Verified by: STEPS 9 and 10.
        cache.invalidateAll().await().indefinitely();
        assertKeySetContains();
        assertGetIfPresentMissingKey(KEY_1);
        assertGetIfPresentMissingKey(KEY_2);

        // STEP 9
        // Action: same call as STEP 6.
        // Expected effect: value loader function invoked because of STEP 8 and result cached.
        // Verified by: different objects references between STEPS 6 and 9 results.
        String value9 = cache.get(KEY_1, k -> new String()).await().indefinitely();
        assertNotSame(value6, value9);
        assertKeySetContains(KEY_1);
        assertGetIfPresent(KEY_1, value9);
        assertGetIfPresentMissingKey(KEY_2);

        // STEP 10
        // Action: same call as STEP 7.
        // Expected effect: value loader function invoked because of STEP 8 and result cached.
        // Verified by: different objects references between STEPS 7 and 10 results.
        String value10 = cache.get(KEY_2, k -> new String()).await().indefinitely();
        assertNotSame(value7, value10);
        assertKeySetContains(KEY_1, KEY_2);
        assertGetIfPresent(KEY_1, value9);
        assertGetIfPresent(KEY_2, value10);
    }

    @Test
    public void testCacheNullValue() throws Exception {
        // with custom key
        Object key = new Object();

        try {
            // when null is cached
            cache.get(key, (k) -> null).await().indefinitely();

            // assert
            assertKeySetContains(key);
            assertGetIfPresent(key, null);
        } finally {
            // invalidate to remove side effects in other tests
            cache.invalidate(key).await().indefinitely();
        }
    }

    @Test
    public void testExceptionInValueLoader() throws Exception {
        // with custom key and exception
        Object key = new Object();
        RuntimeException thrown = new RuntimeException();

        // when exception thrown in the value loader for the key
        RuntimeException result = assertThrows(RuntimeException.class, () -> {
            cache.get(key, k -> {
                throw thrown;
            }).await().indefinitely();
        });

        // assert
        assertSame(thrown, result);
        assertKeySetContains();
        assertGetIfPresentMissingKey(key);
    }

    @Test
    public void testPutShouldPopulateCache() {
        CaffeineCache caffeineCache = cache.as(CaffeineCache.class);
        try {
            caffeineCache.put("foo", CompletableFuture.completedFuture("bar"));
            assertEquals("bar", caffeineCache.get("foo", Function.identity()).await().indefinitely());
        } finally {
            // invalidate to remove side effects in other tests
            cache.invalidate("foo").await().indefinitely();
        }
    }

    private void assertKeySetContains(Object... expectedKeys) {
        Set<Object> expectedKeySet = new HashSet<>(Arrays.asList(expectedKeys));
        Set<Object> actualKeySet = cache.as(CaffeineCache.class).keySet();
        assertEquals(expectedKeySet, actualKeySet);
    }

    private void assertGetIfPresent(Object key, Object value) throws Exception {
        Object actual = cache.as(CaffeineCache.class).getIfPresent(key).get();
        assertSame(value, actual);
    }

    private void assertGetIfPresentMissingKey(Object key) throws Exception {
        CompletableFuture<Object> future = cache.as(CaffeineCache.class).getIfPresent(key);
        assertNull(future);
    }

    @Dependent
    static class CachedService {

        Cache constructorInjectedCache;
        Cache methodInjectedCache;

        public CachedService(@CacheName(CACHE_NAME_1) Cache cache) {
            constructorInjectedCache = cache;
        }

        public Cache getConstructorInjectedCache() {
            return constructorInjectedCache;
        }

        public Cache getMethodInjectedCache() {
            return methodInjectedCache;
        }

        @Inject
        public void setMethodInjectedCache(@CacheName(CACHE_NAME_1) Cache cache) {
            methodInjectedCache = cache;
        }

        @CacheResult(cacheName = CACHE_NAME_1)
        public String cachedMethod(Object key) {
            return new String();
        }

        @CacheInvalidate(cacheName = CACHE_NAME_1)
        public void invalidate(Object key) {
        }

        @CacheInvalidateAll(cacheName = CACHE_NAME_1)
        public void invalidateAll() {
        }
    }
}
