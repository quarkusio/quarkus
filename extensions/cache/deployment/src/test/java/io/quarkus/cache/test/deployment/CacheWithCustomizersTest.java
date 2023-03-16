package io.quarkus.cache.test.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.benmanes.caffeine.cache.Caffeine;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.CaffeineCacheBuilderCustomizer;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheImpl;
import io.quarkus.test.QuarkusUnitTest;

public class CacheWithCustomizersTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot(
            jar -> jar.addClasses(TestResource.class, AppliesToAllCachesCustomizer.class)
                    .addAsResource("cache-config-test.properties", "application.properties"));

    private static final String CACHE_NAME = "test-cache";

    @Inject
    CacheManager cacheManager;

    @CacheName("no-config-cache")
    Cache noConfigCache;

    @CacheName("test-cache-2")
    Cache testCache2;

    @CacheName("test-cache-3")
    Cache testCache3;

    @Test
    void testConfig() {
        CaffeineCacheImpl cache = (CaffeineCacheImpl) cacheManager.getCache(CACHE_NAME).get();
        assertEquals(50, cache.getCacheInfo().initialCapacity);
        assertEquals(100L, cache.getCacheInfo().maximumSize);
    }

    @Test
    void testCache2Config() {
        CaffeineCacheImpl cache = (CaffeineCacheImpl) testCache2;
        assertEquals(50, cache.getCacheInfo().initialCapacity);
        assertNull(cache.getCacheInfo().maximumSize);
    }

    @Test
    void testCache3Config() {
        CaffeineCacheImpl cache = (CaffeineCacheImpl) testCache3;
        assertEquals(50, cache.getCacheInfo().initialCapacity);
        assertNull(cache.getCacheInfo().maximumSize);
    }

    @Singleton
    public static class AppliesToAllCachesCustomizer implements CaffeineCacheBuilderCustomizer {

        @Override
        public void customize(Caffeine<Object, Object> cacheBuilder) {
            cacheBuilder.initialCapacity(50);
        }
    }

    @Path("/test")
    public static class TestResource {

        @GET
        @CacheResult(cacheName = CACHE_NAME)
        public String foo(String key) {
            return "bar";
        }
    }
}
