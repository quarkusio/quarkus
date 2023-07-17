package io.quarkus.cache.test.runtime;

import static io.quarkus.cache.runtime.AbstractCache.NULL_KEYS_NOT_SUPPORTED_MSG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests {@code null} cache keys or values.
 */
public class NullKeyOrValueCacheTest {

    private static final Object KEY = new Object();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot(jar -> jar.addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Test
    public void testNullKeys() {
        assertThrows(NullPointerException.class, () -> {
            cachedService.cachedMethod(null);
        }, NULL_KEYS_NOT_SUPPORTED_MSG);
        assertThrows(NullPointerException.class, () -> {
            cachedService.invalidate(null);
        }, NULL_KEYS_NOT_SUPPORTED_MSG);
    }

    @Test
    public void testNullValues() {
        assertEquals(0, cachedService.getCachedMethodInvocations());

        // STEP 1
        // Action: @CacheResult-annotated method call.
        // Expected effect: method invoked and result cached.
        // Verified by: STEP 2.
        Object value1 = cachedService.cachedMethod(KEY);
        assertEquals(1, cachedService.getCachedMethodInvocations());
        assertNull(value1);

        // STEP 2
        // Action: same call as STEP 1.
        // Expected effect: method not invoked and result coming from the cache.
        // Verified by: same number of cached method invocations between STEPS 1 and 2.
        Object value2 = cachedService.cachedMethod(KEY);
        assertEquals(1, cachedService.getCachedMethodInvocations());
        assertNull(value2);
    }

    @Dependent
    static class CachedService {

        private static final String CACHE_NAME = "test-cache";

        private int cachedMethodInvocations;

        @CacheResult(cacheName = CACHE_NAME)
        public Object cachedMethod(Object key) {
            cachedMethodInvocations++;
            return null;
        }

        @CacheInvalidate(cacheName = CACHE_NAME)
        public void invalidate(Object key) {
        }

        public int getCachedMethodInvocations() {
            return cachedMethodInvocations;
        }
    }
}
