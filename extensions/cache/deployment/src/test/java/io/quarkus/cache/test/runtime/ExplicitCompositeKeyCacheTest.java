package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Locale;

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
 * Tests a cache with <b>explicit composite</b> cache keys.<br>
 * All methods with {@link CacheKey @CacheKey}-annotated arguments also have another argument which is not part of the key.
 */
public class ExplicitCompositeKeyCacheTest {

    private static final Locale KEY_1_ELEMENT_1 = Locale.US;
    private static final BigDecimal KEY_1_ELEMENT_2 = new BigDecimal(123);
    private static final Locale KEY_2_ELEMENT_1 = Locale.FRANCE;
    private static final BigDecimal KEY_2_ELEMENT_2 = new BigDecimal(456);

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot(jar -> jar.addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Test
    public void testAllCacheAnnotations() {

        // In most of the cached service methods calls below, a changing third argument will be passed to the methods.
        // The fact that it changes each time should not have any effect on the cache because it is not part of the cache key.

        // STEP 1
        // Action: @CacheResult-annotated method call.
        // Expected effect: method invoked and result cached.
        // Verified by: STEP 2.
        String value1 = cachedService.cachedMethod(KEY_1_ELEMENT_1, KEY_1_ELEMENT_2, new Object());

        // STEP 2
        // Action: same call as STEP 1.
        // Expected effect: method not invoked and result coming from the cache.
        // Verified by: same object reference between STEPS 1 and 2 results.
        String value2 = cachedService.cachedMethod(KEY_1_ELEMENT_1, KEY_1_ELEMENT_2, new Object());
        assertTrue(value1 == value2);

        // STEP 3
        // Action: same call as STEP 2 with a changing key element.
        // Expected effect: method invoked and result cached.
        // Verified by: different objects references between STEPS 2 and 3 results.
        String value3 = cachedService.cachedMethod(KEY_1_ELEMENT_1, new BigDecimal(789), new Object());
        assertTrue(value2 != value3);

        // STEP 4
        // Action: same principle as STEP 3, but this time we're changing the other key element.
        // Expected effect: method invoked and result cached.
        // Verified by: different objects references between STEPS 2 and 4 results.
        String value4 = cachedService.cachedMethod(Locale.JAPAN, KEY_1_ELEMENT_2, new Object());
        assertTrue(value2 != value4);

        // STEP 5
        // Action: same call as STEP 2 with an entirely new key.
        // Expected effect: method invoked and result cached.
        // Verified by: different objects references between STEPS 2 and 5 results.
        String value5 = cachedService.cachedMethod(KEY_2_ELEMENT_1, KEY_2_ELEMENT_2, new Object());
        assertTrue(value2 != value5);

        // STEP 6
        // Action: cache entry invalidation.
        // Expected effect: STEP 2 cache entry removed.
        // Verified by: STEP 7.
        cachedService.invalidate(new Object(), KEY_1_ELEMENT_1, KEY_1_ELEMENT_2);

        // STEP 7
        // Action: same call as STEP 2.
        // Expected effect: method invoked because of STEP 6 and result cached.
        // Verified by: different objects references between STEPS 2 and 7 results.
        String value7 = cachedService.cachedMethod(KEY_1_ELEMENT_1, KEY_1_ELEMENT_2, new Object());
        assertTrue(value2 != value7);

        // STEP 8
        // Action: same call as STEP 5.
        // Expected effect: method not invoked and result coming from the cache.
        // Verified by: same object reference between STEPS 5 and 8 results.
        String value8 = cachedService.cachedMethod(KEY_2_ELEMENT_1, KEY_2_ELEMENT_2, new Object());
        assertTrue(value5 == value8);

        // STEP 9
        // Action: full cache invalidation.
        // Expected effect: empty cache.
        // Verified by: STEPS 10 and 11.
        cachedService.invalidateAll();

        // STEP 10
        // Action: same call as STEP 7.
        // Expected effect: method invoked because of STEP 9 and result cached.
        // Verified by: different objects references between STEPS 7 and 10 results.
        String value10 = cachedService.cachedMethod(KEY_1_ELEMENT_1, KEY_1_ELEMENT_2, new Object());
        assertTrue(value7 != value10);

        // STEP 11
        // Action: same call as STEP 8.
        // Expected effect: method invoked because of STEP 9 and result cached.
        // Verified by: different objects references between STEPS 8 and 11 results.
        String value11 = cachedService.cachedMethod(KEY_2_ELEMENT_1, KEY_2_ELEMENT_2, new Object());
        assertTrue(value8 != value11);
    }

    @Dependent
    static class CachedService {

        private static final String CACHE_NAME = "test-cache";

        @CacheResult(cacheName = CACHE_NAME)
        public String cachedMethod(@CacheKey Locale keyElement1, @CacheKey BigDecimal keyElement2, Object notPartOfTheKey) {
            return new String();
        }

        @CacheInvalidate(cacheName = CACHE_NAME)
        public void invalidate(Object notPartOfTheKey, @CacheKey Locale keyElement1, @CacheKey BigDecimal keyElement2) {
        }

        @CacheInvalidateAll(cacheName = CACHE_NAME)
        public void invalidateAll() {
        }
    }
}
