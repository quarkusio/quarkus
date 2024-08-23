package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.test.runtime.ImplicitSimpleKeyCacheTest.CachedService;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests a cache shared between different beans.
 */
public class SharedCacheTest {

    private static final Object KEY = new Object();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(CachedService.class, LocalCachedService.class));

    @Inject
    CachedService externalCachedService;

    @Inject
    LocalCachedService localCachedService;

    @Test
    public void testAllCacheAnnotations() {
        // STEP 1
        // Action: @CacheResult-annotated method call.
        // Expected effect: method invoked and result cached in shared cache.
        // Verified by: STEP 2.
        String value1 = externalCachedService.cachedMethod(KEY);

        // STEP 2
        // Action: same call as STEP 1.
        // Expected effect: method not invoked and result coming from the shared cache.
        // Verified by: same object reference between STEPS 1 and 2 results.
        String value2 = externalCachedService.cachedMethod(KEY);
        assertTrue(value1 == value2);

        // STEP 3
        // Action: full shared cache invalidation launched from a different bean than STEPS 1 and 2 calls.
        // Expected effect: empty cache.
        // Verified by: STEPS 4.
        localCachedService.invalidateAll();

        // STEP 4
        // Action: same call as STEP 2.
        // Expected effect: method invoked because of STEP 3 and result cached.
        // Verified by: different objects references between STEPS 2 and 4 results.
        String value4 = externalCachedService.cachedMethod(KEY);
        assertTrue(value2 != value4);
    }

    @Dependent
    static class LocalCachedService {

        @CacheInvalidateAll(cacheName = CachedService.CACHE_NAME)
        public void invalidateAll() {
        }
    }
}
