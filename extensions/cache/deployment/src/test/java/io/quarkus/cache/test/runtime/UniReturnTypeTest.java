package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

/**
 * Tests the caching annotations on methods returning {@link Uni}.
 */
public class UniReturnTypeTest {

    private static final String CACHE_NAME_1 = "test-cache-1";
    private static final String CACHE_NAME_2 = "test-cache-2";
    private static final String KEY_1 = "key-1";
    private static final String KEY_2 = "key-2";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot((jar) -> jar.addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Test
    void testCacheResult() {
        // STEP 1
        // Action: a method annotated with @CacheResult and returning a Uni is called.
        // Expected effect: the method is not invoked, as Uni is lazy.
        // Verified by: invocations counter.
        Uni<String> uni1 = cachedService.cacheResult1(KEY_1);
        assertEquals(0, cachedService.getCacheResultInvocations());

        // STEP 2
        // Action: same call as STEP 1.
        // Expected effect: same as STEP 1 with a different Uni instance returned.
        // Verified by: invocations counter and different objects references between STEPS 1 AND 2 results.
        Uni<String> uni2 = cachedService.cacheResult1(KEY_1);
        assertEquals(0, cachedService.getCacheResultInvocations());
        assertNotSame(uni1, uni2);

        // STEP 3
        // Action: the Uni returned in STEP 1 is subscribed to and we wait for an item event to be fired.
        // Expected effect: the method from STEP 1 is invoked and its result is cached.
        // Verified by: invocations counter and STEP 4.
        String emittedItem1 = uni1.await().indefinitely();
        assertEquals(1, cachedService.getCacheResultInvocations());

        // STEP 4
        // Action: the Uni returned in STEP 2 is subscribed to and we wait for an item event to be fired.
        // Expected effect: the method from STEP 2 is not invoked and the value cached in STEP 3 is returned.
        // Verified by: invocations counter and same object reference between STEPS 3 and 4 emitted items.
        String emittedItem2 = uni2.await().indefinitely();
        assertEquals(1, cachedService.getCacheResultInvocations());
        assertSame(emittedItem1, emittedItem2);

        // STEP 5
        // Action: same call as STEP 2 with a different key and an immediate subscription.
        // Expected effect: the method is invoked and a new item is emitted (also cached).
        // Verified by: invocations counter and different objects references between STEPS 2 and 3 emitted items.
        String emittedItem3 = cachedService.cacheResult1("another-key").await().indefinitely();
        assertEquals(2, cachedService.getCacheResultInvocations());
        assertNotSame(emittedItem2, emittedItem3);
    }

    @Test
    void testCacheInvalidate() {
        // First, let's put some data into the caches.
        String value1 = cachedService.cacheResult1(KEY_1).await().indefinitely();
        Object value2 = cachedService.cacheResult2(KEY_1).await().indefinitely();
        Object value3 = cachedService.cacheResult2(KEY_2).await().indefinitely();

        // We will invalidate some data (only KEY_1) in all caches later.
        Uni<Void> invalidateUni = cachedService.cacheInvalidate(KEY_1);
        // For now, the method that will invalidate the data should not be invoked, as Uni is lazy.
        assertEquals(0, cachedService.getCacheInvalidateInvocations());

        // The data should still be cached at this point.
        String value4 = cachedService.cacheResult1(KEY_1).await().indefinitely();
        Object value5 = cachedService.cacheResult2(KEY_1).await().indefinitely();
        Object value6 = cachedService.cacheResult2(KEY_2).await().indefinitely();
        assertSame(value1, value4);
        assertSame(value2, value5);
        assertSame(value3, value6);

        // It's time to perform the data invalidation.
        invalidateUni.await().indefinitely();
        // The method annotated with @CacheInvalidate should have been invoked now.
        assertEquals(1, cachedService.getCacheInvalidateInvocations());

        // Let's call the methods annotated with @CacheResult again.
        String value7 = cachedService.cacheResult1(KEY_1).await().indefinitely();
        Object value8 = cachedService.cacheResult2(KEY_1).await().indefinitely();
        Object value9 = cachedService.cacheResult2(KEY_2).await().indefinitely();

        // The objects references should be different for the invalidated key.
        assertNotSame(value4, value7);
        assertNotSame(value5, value8);
        // The object reference should remain unchanged for the key that was not invalidated.
        assertSame(value6, value9);
    }

    @Test
    void testCacheInvalidateAll() {
        // First, let's put some data into the caches.
        String value1 = cachedService.cacheResult1(KEY_1).await().indefinitely();
        Object value2 = cachedService.cacheResult2(KEY_2).await().indefinitely();

        // We will invalidate all the data in all caches later.
        Uni<Void> invalidateAllUni = cachedService.cacheInvalidateAll();
        // For now, the method that will invalidate the data should not be invoked, as Uni is lazy.
        assertEquals(0, cachedService.getCacheInvalidateAllInvocations());

        // The data should still be cached at this point.
        String value3 = cachedService.cacheResult1(KEY_1).await().indefinitely();
        Object value4 = cachedService.cacheResult2(KEY_2).await().indefinitely();
        assertSame(value1, value3);
        assertSame(value2, value4);

        // It's time to perform the data invalidation.
        invalidateAllUni.await().indefinitely();
        // The method annotated with @CacheInvalidateAll should have been invoked now.
        assertEquals(1, cachedService.getCacheInvalidateAllInvocations());

        // Let's call the methods annotated with @CacheResult again.
        String value5 = cachedService.cacheResult1(KEY_1).await().indefinitely();
        Object value6 = cachedService.cacheResult2(KEY_2).await().indefinitely();

        // All objects references should be different.
        assertNotSame(value1, value5);
        assertNotSame(value2, value6);
    }

    @ApplicationScoped
    static class CachedService {

        private volatile int cacheResultInvocations;
        private volatile int cacheInvalidateInvocations;
        private volatile int cacheInvalidateAllInvocations;

        @CacheResult(cacheName = CACHE_NAME_1)
        public Uni<String> cacheResult1(String key) {
            cacheResultInvocations++;
            return Uni.createFrom().item(() -> new String());
        }

        @CacheResult(cacheName = CACHE_NAME_2)
        public Uni<Object> cacheResult2(String key) {
            return Uni.createFrom().item(() -> new Object());
        }

        @CacheInvalidate(cacheName = CACHE_NAME_1)
        @CacheInvalidate(cacheName = CACHE_NAME_2)
        public Uni<Void> cacheInvalidate(String key) {
            cacheInvalidateInvocations++;
            return Uni.createFrom().nullItem();
        }

        @CacheInvalidateAll(cacheName = CACHE_NAME_1)
        @CacheInvalidateAll(cacheName = CACHE_NAME_2)
        public Uni<Void> cacheInvalidateAll() {
            cacheInvalidateAllInvocations++;
            return Uni.createFrom().nullItem();
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
