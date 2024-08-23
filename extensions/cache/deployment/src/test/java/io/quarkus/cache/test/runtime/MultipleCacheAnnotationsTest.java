package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests multiple caching annotations on a single method.
 */
public class MultipleCacheAnnotationsTest {

    private static final Object KEY = new Object();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot(jar -> jar.addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Test
    public void testAllCacheAnnotations() {
        // STEP 1
        // Action: @CacheResult(cache1)-annotated method call.
        // Effect: method invoked and result cached into `cache1`.
        String value1 = cachedService.cachedMethod1(KEY);

        // STEP 2
        // Action: [@CacheResult(cache1) and @CacheInvalidate(cache1)]-annotated method call.
        // Expected effect: cache entry invalidated, then method invoked and result cached into `cache1`.
        // Verified by: different objects references between STEPS 1 and 2 results.
        String value2 = cachedService.cachedMethodWithInvalidate(KEY);
        assertTrue(value1 != value2);

        // STEP 3
        // Action: @CacheResult(cache2)-annotated method call.
        // Effect: method invoked and result cached into `cache2`.
        String value3 = cachedService.cachedMethod2(KEY);

        // STEP 4
        // Action: [@CacheResult(cache1) and @CacheInvalidateAll(cache2)]-annotated method call.
        // Expected effect: all entries invalidated in `cache2`, then method not invoked and result coming from `cache1`.
        // Verified by: STEP 5 and same object reference between STEPS 2 and 4 results.
        String value4 = cachedService.cachedMethodWithInvalidateAll(KEY);
        assertTrue(value2 == value4);

        // STEP 5
        // Action: @CacheResult(cache2)-annotated method call.
        // Expected effect: method invoked because of STEP 4 and result cached into `cache2`.
        // Verified by: different objects references between STEPS 3 AND 5 results.
        String value5 = cachedService.cachedMethod2(KEY);
        assertTrue(value3 != value5);

        // At this point, we know that `cache1` and `cache2` both contain one entry (value4 and value5)

        // STEP 6
        // Action: [@CacheInvalidate(cache1) and @CacheInvalidate(cache2)]-annotated method call.
        // Expected effect: cache entry invalidated in `cache1` and `cache2`.
        // Verified by: STEPS 7 and 8.
        cachedService.multipleCacheInvalidate(KEY);

        // STEP 7
        // Action: @CacheResult(cache1)-annotated method call.
        // Expected effect: method invoked because of STEP 6 and result cached into `cache1`.
        // Verified by: different objects references between STEPS 4 and 7 results.
        String value7 = cachedService.cachedMethod1(KEY);
        assertTrue(value4 != value7);

        // STEP 8
        // Action: @CacheResult(cache2)-annotated method call.
        // Expected effect: method invoked because of STEP 6 and result cached into `cache2`.
        // Verified by: different objects references between STEPS 5 and 8 results.
        String value8 = cachedService.cachedMethod2(KEY);
        assertTrue(value5 != value8);

        // STEP 9
        // Action: [@CacheInvalidateAll(cache1) and @CacheInvalidateAll(cache2)]-annotated method call.
        // Expected effect: `cache1` and `cache2` are emptied.
        // Verified by: STEPS 10 and 11.
        cachedService.multipleCacheInvalidateAll();

        // STEP 10
        // Action: @CacheResult(cache1)-annotated method call.
        // Expected effect: method invoked because of STEP 9 and result cached into `cache1`.
        // Verified by: different objects references between STEPS 7 and 10 results.
        String value10 = cachedService.cachedMethod1(KEY);
        assertTrue(value7 != value10);

        // STEP 11
        // Action: @CacheResult(cache2)-annotated method call.
        // Expected effect: method invoked because of STEP 9 and result cached into `cache2`.
        // Verified by: different objects references between STEPS 8 and 11 results.
        String value11 = cachedService.cachedMethod2(KEY);
        assertTrue(value8 != value11);
    }

    @Test
    void testMultipleCacheInvalidateWithNoArg() {
        // STEP 1
        // Action: @CacheResult(cache1)-annotated method call.
        // Effect: method invoked, default cache key used and result cached into `cache1`.
        String value1 = cachedService.cachedMethodWithNoArg1();

        // STEP 2
        // Action: @CacheResult(cache2)-annotated method call.
        // Effect: method invoked, default cache key used and result cached into `cache2`.
        String value2 = cachedService.cachedMethodWithNoArg2();

        // STEP 3
        // Action: [@CacheInvalidate(cache1) and @CacheInvalidate(cache2)]-annotated method call.
        // Expected effect: default cache key used and cache entry invalidated in `cache1` and `cache2`.
        // Verified by: STEPS 4 and 5.
        cachedService.multipleCacheInvalidateWithNoArg();

        // STEP 4
        // Action: @CacheResult(cache1)-annotated method call.
        // Expected effect: method invoked because of STEP 3, default cache key used and result cached into `cache1`.
        // Verified by: different objects references between STEPS 1 and 4 results.
        String value4 = cachedService.cachedMethodWithNoArg1();
        assertNotSame(value1, value4);

        // STEP 5
        // Action: @CacheResult(cache2)-annotated method call.
        // Expected effect: method invoked because of STEP 3, default cache key used and result cached into `cache2`.
        // Verified by: different objects references between STEPS 2 and 5 results.
        String value5 = cachedService.cachedMethodWithNoArg2();
        assertNotSame(value2, value5);
    }

    @ApplicationScoped
    static class CachedService {

        static final String TEST_CACHE_1 = "cache1";
        static final String TEST_CACHE_2 = "cache2";

        @CacheResult(cacheName = TEST_CACHE_1)
        public String cachedMethod1(Object key) {
            return new String();
        }

        @CacheResult(cacheName = TEST_CACHE_2)
        public String cachedMethod2(Object key) {
            return new String();
        }

        @CacheResult(cacheName = TEST_CACHE_1)
        public String cachedMethodWithNoArg1() {
            return new String();
        }

        @CacheResult(cacheName = TEST_CACHE_2)
        public String cachedMethodWithNoArg2() {
            return new String();
        }

        @CacheResult(cacheName = TEST_CACHE_1)
        @CacheInvalidate(cacheName = TEST_CACHE_1)
        public String cachedMethodWithInvalidate(Object key) {
            return new String();
        }

        @CacheResult(cacheName = TEST_CACHE_1)
        @CacheInvalidateAll(cacheName = TEST_CACHE_2)
        public String cachedMethodWithInvalidateAll(Object key) {
            return new String();
        }

        @CacheInvalidate(cacheName = TEST_CACHE_1)
        @CacheInvalidate(cacheName = TEST_CACHE_2)
        public void multipleCacheInvalidate(Object key) {
        }

        @CacheInvalidate(cacheName = TEST_CACHE_1)
        @CacheInvalidate(cacheName = TEST_CACHE_2)
        public void multipleCacheInvalidateWithNoArg() {
        }

        @CacheInvalidateAll(cacheName = TEST_CACHE_1)
        @CacheInvalidateAll(cacheName = TEST_CACHE_2)
        public void multipleCacheInvalidateAll() {
        }
    }
}
