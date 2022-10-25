package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.runtime.noop.NoOpCache;
import io.quarkus.test.QuarkusUnitTest;

public class NoOpCacheTest {

    private static final String CACHE_NAME = "test-cache";
    private static final Object KEY = new Object();
    private static final String FORCED_EXCEPTION_MESSAGE = "Forced exception";

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

    @Test
    public void testAllCacheAnnotationsAndMethods() {
        // STEP 1
        // Action: @CacheResult-annotated method call.
        // Expected effect: method invoked and result not cached.
        // Verified by: STEP 2.
        String value1 = cachedService.cachedMethod(KEY);

        // STEP 2
        // Action: same call as STEP 1.
        // Expected effect: method invoked and result not cached.
        // Verified by: different objects references between STEPS 1 and 2 results.
        String value2 = cachedService.cachedMethod(KEY);
        assertNotSame(value1, value2);

        // STEP 3
        // Action: value retrieval from the cache with the same key as STEP 2.
        // Expected effect: value loader function invoked and result not cached.
        // Verified by: different objects references between STEPS 2 and 3 results.
        String value3 = cache.get(KEY, k -> new String()).await().indefinitely();
        assertNotSame(value2, value3);

        // STEP 4
        // Action: value retrieval from the cache with the same key as STEP 3.
        // Expected effect: value loader function invoked and result not cached.
        // Verified by: different objects references between STEPS 3 and 4 results.
        String value4 = cache.get(KEY, k -> new String()).await().indefinitely();
        assertNotSame(value3, value4);

        // The following methods have no effect at all, but let's check if they're running fine anyway.
        cachedService.invalidate(KEY);
        cachedService.invalidateAll();
        cache.invalidate(KEY).await().indefinitely();
        cache.invalidateAll().await().indefinitely();
    }

    @Test
    public void testRuntimeExceptionThrowDuringCacheComputation() {
        NumberFormatException e = assertThrows(NumberFormatException.class, () -> {
            cachedService.throwRuntimeExceptionDuringCacheComputation();
        });
        assertEquals(FORCED_EXCEPTION_MESSAGE, e.getMessage());
    }

    @Test
    public void testCheckedExceptionThrowDuringCacheComputation() {
        IOException e = assertThrows(IOException.class, () -> {
            cachedService.throwCheckedExceptionDuringCacheComputation();
        });
        assertEquals(FORCED_EXCEPTION_MESSAGE, e.getMessage());
    }

    @Test
    public void testErrorThrowDuringCacheComputation() {
        OutOfMemoryError e = assertThrows(OutOfMemoryError.class, () -> {
            cachedService.throwErrorDuringCacheComputation();
        });
        assertEquals(FORCED_EXCEPTION_MESSAGE, e.getMessage());
    }

    @Singleton
    static class CachedService {

        @CacheResult(cacheName = CACHE_NAME)
        public String cachedMethod(Object key) {
            return new String();
        }

        @CacheInvalidate(cacheName = CACHE_NAME)
        public void invalidate(Object key) {
        }

        @CacheInvalidateAll(cacheName = CACHE_NAME)
        public void invalidateAll() {
        }

        @CacheResult(cacheName = "runtime-exception-cache")
        public String throwRuntimeExceptionDuringCacheComputation() {
            throw new NumberFormatException(FORCED_EXCEPTION_MESSAGE);
        }

        @CacheResult(cacheName = "checked-exception-cache")
        public String throwCheckedExceptionDuringCacheComputation() throws IOException {
            throw new IOException(FORCED_EXCEPTION_MESSAGE);
        }

        @CacheResult(cacheName = "error-cache")
        public String throwErrorDuringCacheComputation() {
            throw new OutOfMemoryError(FORCED_EXCEPTION_MESSAGE);
        }
    }
}
