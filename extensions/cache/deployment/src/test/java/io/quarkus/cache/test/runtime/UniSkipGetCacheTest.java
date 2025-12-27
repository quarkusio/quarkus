package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

/**
 * Tests cache <b>skipGet</b> option.
 */
public class UniSkipGetCacheTest {

    private static final long KEY_1 = 123L;

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot(jar -> jar.addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Test
    public void testAllCacheAnnotations() {

        // STEP 1
        // Action: @CacheResult-annotated method call.
        // Expected effect: method invoked and result cached.
        // Verified by: STEP 2.
        String value1 = cachedService.cachedMethodWithSkipGet(KEY_1, new Object()).await().indefinitely();

        // STEP 2
        // Action: same call as STEP 1.
        // Expected effect: method invoked and result put into the cache.
        // Verified by: different object reference between STEPS 1 and 2 results.
        String value2 = cachedService.cachedMethodWithSkipGet(KEY_1, new Object()).await().indefinitely();
        assertNotEquals(value1, value2);

        // STEP 3
        // Action: @CacheResult-annotated method call.
        // Expected effect: method not invoked and result taken from the cache.
        // Verified by: same object reference between STEPS 2 and 3 results, but not STEP 1
        String value3 = cachedService.cachedMethodWithoutSkipGet(KEY_1).await().indefinitely();
        assertNotEquals(value1, value3);
        assertEquals(value2, value3);
    }

    @Dependent
    static class CachedService {

        private static final String CACHE_NAME = "test-cache";

        @CacheResult(cacheName = CACHE_NAME, skipGet = true)
        public Uni<String> cachedMethodWithSkipGet(@CacheKey long key, Object notPartOfTheKey) {
            return Uni.createFrom().item(UUID.randomUUID().toString());
        }

        @CacheResult(cacheName = CACHE_NAME, skipGet = false)
        public Uni<String> cachedMethodWithoutSkipGet(@CacheKey long key) {
            return Uni.createFrom().item(UUID.randomUUID().toString());
        }
    }
}
