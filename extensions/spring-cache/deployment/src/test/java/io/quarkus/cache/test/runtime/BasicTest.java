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
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests a cache with a <b>default</b> cache key.
 */
public class BasicTest {

    private static final Object KEY = new Object();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Test
    public void testAllCacheAnnotations() {
        // STEP 1
        // Action: no-arg @Cacheable-annotated method call.
        // Expected effect: method invoked and result cached.
        // Verified by: STEP 2.
        String value1 = cachedService.cachedMethod();

        // STEP 2
        // Action: same call as STEP 1.
        // Expected effect: method not invoked and result coming from the cache.
        // Verified by: same object reference between STEPS 1 and 2 results.
        String value2 = cachedService.cachedMethod();
        assertSame(value1, value2);

        // STEP 3
        // Action: @Cacheable-annotated method call with a key argument.
        // Expected effect: method invoked and result cached.
        // Verified by: different objects references between STEPS 2 and 3 results.
        String value3 = cachedService.cachedMethodWithKey(KEY);
        assertNotSame(value2, value3);

        // STEP 4
        // Action: default key cache entry invalidation.
        // Expected effect: STEP 2 cache entry removed.
        // Verified by: STEP 5.
        cachedService.invalidate();

        // STEP 5
        // Action: same call as STEP 2.
        // Expected effect: method invoked because of STEP 4 and result cached.
        // Verified by: different objects references between STEPS 2 and 5 results.
        String value5 = cachedService.cachedMethod();
        assertNotSame(value2, value5);

        // STEP 6
        // Action: same call as STEP 3.
        // Expected effect: method not invoked and result coming from the cache.
        // Verified by: same object reference between STEPS 3 and 6 results.
        String value6 = cachedService.cachedMethodWithKey(KEY);
        assertSame(value3, value6);

        // STEP 7
        // Action: full cache invalidation.
        // Expected effect: empty cache.
        // Verified by: STEPS 8 and 9.
        cachedService.invalidateAll();

        // STEP 8
        // Action: same call as STEP 5.
        // Expected effect: method invoked because of STEP 7 and result cached.
        // Verified by: different objects references between STEPS 5 and 8 results.
        String value8 = cachedService.cachedMethod();
        assertNotSame(value5, value8);

        // STEP 9
        // Action: same call as STEP 6.
        // Expected effect: method invoked because of STEP 7 and result cached.
        // Verified by: different objects references between STEPS 6 and 9 results.
        String value9 = cachedService.cachedMethodWithKey(KEY);
        assertNotSame(value6, value9);

        // STEP 10
        // Action: @CachePut-annotated method call.
        // Expected effect: previous cache entry invalidated and new result added to the cache
        // Verified by: different objects references between STEPS 9 and 10 results. The addition to the cache is validated by STEP 11
        String value10 = cachedService.cachePutMethod(KEY);
        assertNotSame(value10, value9);

        // STEP 11
        // Action: @Cacheable-annotated method call.
        // Expected effect: read cached data from previous call (since the same cache is used)
        String value11 = cachedService.cachedMethodWithKey(KEY);
        assertSame(value11, value10);
    }

    @Singleton
    static class CachedService {

        private static final String CACHE_NAME = "test-cache";

        @Cacheable(cacheNames = CACHE_NAME)
        public String cachedMethod() {
            return new String();
        }

        @Cacheable(CACHE_NAME)
        public String cachedMethodWithKey(Object key) {
            return new String();
        }

        @CachePut(CACHE_NAME)
        public String cachePutMethod(Object key) {
            return new String();
        }

        @CacheEvict(cacheNames = CACHE_NAME)
        public void invalidate() {
        }

        @CacheEvict(value = CACHE_NAME, allEntries = true)
        public void invalidateAll() {
        }
    }
}
