package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;

public class NoOpCacheTest {

    private static final String CACHE_NAME = "test-cache";
    private static final Object KEY = new Object();

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
            .addAsResource(new StringAsset("quarkus.cache.enabled=false"), "application.properties")
            .addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Test
    public void testAllCacheAnnotations() {
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
        assertTrue(value1 != value2);

        // The following methods have no effect at all, but let's check if they're running fine anyway.
        cachedService.invalidate(KEY);
        cachedService.invalidateAll();
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
    }
}
