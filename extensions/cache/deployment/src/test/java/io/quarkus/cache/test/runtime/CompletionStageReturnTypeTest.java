package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests the caching annotations on methods returning {@link CompletableFuture}.
 */
public class CompletionStageReturnTypeTest {

    private static final String CACHE_NAME_1 = "test-cache-1";
    private static final String CACHE_NAME_2 = "test-cache-2";
    private static final String KEY_1 = "key-1";
    private static final String KEY_2 = "key-2";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot((jar) -> jar.addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Test
    void testCacheResult() throws ExecutionException, InterruptedException {
        // STEP 1
        // Action: a method annotated with @CacheResult and returning a CompletableFuture is called.
        // Expected effect: the method is invoked and its result is cached asynchronously, as CompletableFuture is eager.
        // Verified by: invocations counter.
        CompletableFuture<String> cf1 = cachedService.cacheResult1(KEY_1);
        assertEquals(1, cachedService.getCacheResultInvocations());

        // STEP 2
        // Action: same call as STEP 1.
        // Expected effect: the method is not invoked and a new CompletableFuture instance is returned (because of the cache interceptor implementation).
        // Verified by: invocations counter and different objects references between STEPS 1 AND 2 results.
        CompletableFuture<String> cf2 = cachedService.cacheResult1(KEY_1);
        assertEquals(1, cachedService.getCacheResultInvocations());
        assertNotSame(cf1, cf2);

        // STEP 3
        // Action: the result of the CompletableFuture from STEP 1 is retrieved.
        // Expected effect: the method from STEP 1 is not invoked and the value cached in STEP 1 is returned.
        // Verified by: invocations counter and STEP 4.
        String result1 = cf1.get();
        assertEquals(1, cachedService.getCacheResultInvocations());

        // STEP 4
        // Action: the result of the CompletableFuture from STEP 2 is retrieved.
        // Expected effect: the method from STEP 2 is not invoked and the value cached in STEP 1 is returned.
        // Verified by: invocations counter and same object reference between STEPS 3 and 4 emitted items.
        String result2 = cf2.get();
        assertEquals(1, cachedService.getCacheResultInvocations());
        assertSame(result1, result2);

        // STEP 5
        // Action: same call as STEP 2 with a different key and an immediate CompletableFuture result retrieval.
        // Expected effect: the method is invoked and a new value is cached.
        // Verified by: invocations counter and different objects references between STEPS 2 and 5 results.
        String result3 = cachedService.cacheResult1("another-key").get();
        assertEquals(2, cachedService.getCacheResultInvocations());
        assertNotSame(result2, result3);
    }

    @Test
    void testCacheInvalidate() throws ExecutionException, InterruptedException {
        // First, let's put some data into the caches.
        String value1 = cachedService.cacheResult1(KEY_1).get();
        Object value2 = cachedService.cacheResult2(KEY_1).get();
        Object value3 = cachedService.cacheResult2(KEY_2).get();

        // The cached data identified by KEY_1 is invalidated now.
        cachedService.cacheInvalidate(KEY_1).get();
        // The method annotated with @CacheInvalidate should have been invoked, as CompletionFuture is eager.
        assertEquals(1, cachedService.getCacheInvalidateInvocations());

        // The data for the second key should still be cached at this point.
        Object value4 = cachedService.cacheResult2(KEY_2).get();
        assertSame(value3, value4);

        // The data identified by KEY_1 should have been removed from the cache.
        String value5 = cachedService.cacheResult1(KEY_1).get();
        Object value6 = cachedService.cacheResult2(KEY_1).get();

        // The objects references should be different for the invalidated key.
        assertNotSame(value1, value5);
        assertNotSame(value2, value6);
    }

    @Test
    void testCacheInvalidateAll() throws ExecutionException, InterruptedException {
        // First, let's put some data into the caches.
        String value1 = cachedService.cacheResult1(KEY_1).get();
        Object value2 = cachedService.cacheResult2(KEY_2).get();

        // All the cached data is invalidated now.
        cachedService.cacheInvalidateAll().get();

        // The method annotated with @CacheInvalidateAll should have been invoked, as CompletionStage is eager.
        assertEquals(1, cachedService.getCacheInvalidateAllInvocations());

        // Let's call the methods annotated with @CacheResult again.
        String value3 = cachedService.cacheResult1(KEY_1).get();
        Object value4 = cachedService.cacheResult2(KEY_2).get();

        // All objects references should be different.
        assertNotSame(value1, value3);
        assertNotSame(value2, value4);
    }

    @ApplicationScoped
    static class CachedService {

        private volatile int cacheResultInvocations;
        private volatile int cacheInvalidateInvocations;
        private volatile int cacheInvalidateAllInvocations;

        @CacheResult(cacheName = CACHE_NAME_1)
        public CompletableFuture<String> cacheResult1(String key) {
            cacheResultInvocations++;
            return CompletableFuture.completedFuture(new String());
        }

        @CacheResult(cacheName = CACHE_NAME_2)
        public CompletableFuture<Object> cacheResult2(String key) {
            return CompletableFuture.completedFuture(new Object());
        }

        @CacheInvalidate(cacheName = CACHE_NAME_1)
        @CacheInvalidate(cacheName = CACHE_NAME_2)
        public CompletableFuture<Void> cacheInvalidate(String key) {
            cacheInvalidateInvocations++;
            return CompletableFuture.completedFuture(null);
        }

        @CacheInvalidateAll(cacheName = CACHE_NAME_1)
        @CacheInvalidateAll(cacheName = CACHE_NAME_2)
        public CompletableFuture<Void> cacheInvalidateAll() {
            cacheInvalidateAllInvocations++;
            return CompletableFuture.completedFuture(null);
        }

        public int getCacheResultInvocations() {
            return cacheResultInvocations;
        }

        public int getCacheInvalidateInvocations() {
            return cacheInvalidateInvocations;
        }

        public int getCacheInvalidateAllInvocations() {
            return cacheInvalidateAllInvocations;
        }
    }
}
