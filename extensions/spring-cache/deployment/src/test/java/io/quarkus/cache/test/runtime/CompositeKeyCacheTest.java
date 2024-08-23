package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import io.quarkus.test.QuarkusUnitTest;

public class CompositeKeyCacheTest {

    private static final String KEY_1_ELEMENT_1 = "foo";
    private static final int KEY_1_ELEMENT_2 = 123;
    private static final String KEY_2_ELEMENT_1 = "bar";
    private static final int KEY_2_ELEMENT_2 = 456;

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Test
    public void testAllCacheAnnotations() {
        // STEP 1
        // Action: @CacheResult-annotated method call.
        // Expected effect: method invoked and result cached.
        // Verified by: STEP 2.
        String value1 = cachedService.cachedMethod(KEY_1_ELEMENT_1, KEY_1_ELEMENT_2);

        // STEP 2
        // Action: same call as STEP 1.
        // Expected effect: method not invoked and result coming from the cache.
        // Verified by: same object reference between STEPS 1 and 2 results.
        String value2 = cachedService.cachedMethod(KEY_1_ELEMENT_1, KEY_1_ELEMENT_2);
        assertSame(value1, value2);

        // STEP 3
        // Action: same call as STEP 2 with a changing key element.
        // Expected effect: method invoked and result cached.
        // Verified by: different objects references between STEPS 2 and 3 results.
        String value3 = cachedService.cachedMethod(KEY_1_ELEMENT_1, 789);
        assertNotSame(value2, value3);

        // STEP 4
        // Action: same principle as STEP 3, but this time we're changing the other key element.
        // Expected effect: method invoked and result cached.
        // Verified by: different objects references between STEPS 2 and 4 results.
        String value4 = cachedService.cachedMethod("quarkus", KEY_1_ELEMENT_2);
        assertNotSame(value2, value4);

        // STEP 5
        // Action: same call as STEP 2 with an entirely new key.
        // Expected effect: method invoked and result cached.
        // Verified by: different objects references between STEPS 2 and 5 results.
        String value5 = cachedService.cachedMethod(KEY_2_ELEMENT_1, KEY_2_ELEMENT_2);
        assertNotSame(value2, value5);

        // STEP 6
        // Action: cache entry invalidation.
        // Expected effect: STEP 2 cache entry removed.
        // Verified by: STEP 7.
        cachedService.invalidate(KEY_1_ELEMENT_1, KEY_1_ELEMENT_2);

        // STEP 7
        // Action: same call as STEP 2.
        // Expected effect: method invoked because of STEP 6 and result cached.
        // Verified by: different objects references between STEPS 2 and 7 results.
        String value7 = cachedService.cachedMethod(KEY_1_ELEMENT_1, KEY_1_ELEMENT_2);
        assertNotSame(value2, value7);

        // STEP 8
        // Action: same call as STEP 5.
        // Expected effect: method not invoked and result coming from the cache.
        // Verified by: same object reference between STEPS 5 and 8 results.
        String value8 = cachedService.cachedMethod(KEY_2_ELEMENT_1, KEY_2_ELEMENT_2);
        assertSame(value5, value8);

        // STEP 9
        // Action: full cache invalidation.
        // Expected effect: empty cache.
        // Verified by: STEPS 10 and 11.
        cachedService.invalidateAll();

        // STEP 10
        // Action: same call as STEP 7.
        // Expected effect: method invoked because of STEP 9 and result cached.
        // Verified by: different objects references between STEPS 7 and 10 results.
        String value10 = cachedService.cachedMethod(KEY_1_ELEMENT_1, KEY_1_ELEMENT_2);
        assertNotSame(value7, value10);

        // STEP 11
        // Action: same call as STEP 8.
        // Expected effect: method invoked because of STEP 9 and result cached.
        // Verified by: different objects references between STEPS 8 and 11 results.
        String value11 = cachedService.cachedMethod(KEY_2_ELEMENT_1, KEY_2_ELEMENT_2);
        assertNotSame(value8, value11);
    }

    @Singleton
    static class CachedService {

        private static final String CACHE_NAME = "test-cache";

        @Cacheable(CACHE_NAME)
        public String cachedMethod(String keyElement1, int keyElement2) {
            return new String();
        }

        @CacheEvict(cacheNames = CACHE_NAME)
        public void invalidate(String keyElement1, int keyElement2) {
        }

        @CacheEvict(value = CACHE_NAME, allEntries = true)
        public void invalidateAll() {
        }
    }
}
