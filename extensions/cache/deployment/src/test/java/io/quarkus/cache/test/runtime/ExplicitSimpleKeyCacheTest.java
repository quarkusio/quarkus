package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests a cache with <b>explicit simple</b> cache keys.<br>
 * All methods with a {@link CacheKey @CacheKey}-annotated argument also have another argument which is not part of the key.
 */
public class ExplicitSimpleKeyCacheTest {

    private static final long KEY_1 = 123L;
    private static final long KEY_2 = 456L;

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot(jar -> jar.addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Test
    public void testAllCacheAnnotations() {

        // In most of the cached service methods calls below, a changing second argument will be passed to the methods.
        // The fact that it changes each time should not have any effect on the cache because it is not part of the cache key.

        // STEP 1
        // Action: @CacheResult-annotated method call.
        // Expected effect: method invoked and result cached.
        // Verified by: STEP 2.
        String value1 = cachedService.cachedMethod(KEY_1, new Object());

        // STEP 2
        // Action: same call as STEP 1.
        // Expected effect: method not invoked and result coming from the cache.
        // Verified by: same object reference between STEPS 1 and 2 results.
        String value2 = cachedService.cachedMethod(KEY_1, new Object());
        assertTrue(value1 == value2);

        // STEP 3
        // Action: same call as STEP 2 with a new key.
        // Expected effect: method invoked and result cached.
        // Verified by: different objects references between STEPS 2 and 3 results.
        String value3 = cachedService.cachedMethod(KEY_2, new Object());
        assertTrue(value2 != value3);

        // STEP 4
        // Action: cache entry invalidation.
        // Expected effect: STEP 2 cache entry removed.
        // Verified by: STEP 5.
        cachedService.invalidate(KEY_1, new Object());

        // STEP 5
        // Action: same call as STEP 2.
        // Expected effect: method invoked because of STEP 4 and result cached.
        // Verified by: different objects references between STEPS 2 and 5 results.
        String value5 = cachedService.cachedMethod(KEY_1, new Object());
        assertTrue(value2 != value5);

        // STEP 6
        // Action: same call as STEP 3.
        // Expected effect: method not invoked and result coming from the cache.
        // Verified by: same object reference between STEPS 3 and 6 results.
        String value6 = cachedService.cachedMethod(KEY_2, new Object());
        assertTrue(value3 == value6);

        // STEP 7
        // Action: full cache invalidation.
        // Expected effect: empty cache.
        // Verified by: STEPS 8 and 9.
        cachedService.invalidateAll();

        // STEP 8
        // Action: same call as STEP 5.
        // Expected effect: method invoked because of STEP 7 and result cached.
        // Verified by: different objects references between STEPS 5 and 8 results.
        String value8 = cachedService.cachedMethod(KEY_1, new Object());
        assertTrue(value5 != value8);

        // STEP 9
        // Action: same call as STEP 6.
        // Expected effect: method invoked because of STEP 7 and result cached.
        // Verified by: different objects references between STEPS 6 and 9 results.
        String value9 = cachedService.cachedMethod(KEY_2, new Object());
        assertTrue(value6 != value9);
    }

    @Dependent
    static class CachedService {

        private static final String CACHE_NAME = "test-cache";

        @CacheResult(cacheName = CACHE_NAME)
        public String cachedMethod(@CacheKey long key, Object notPartOfTheKey) {
            return new String();
        }

        @CacheInvalidate(cacheName = CACHE_NAME)
        public void invalidate(@CacheKey long key, Object notPartOfTheKey) {
        }

        @CacheInvalidateAll(cacheName = CACHE_NAME)
        public void invalidateAll() {
        }
    }
}
