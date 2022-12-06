package io.quarkus.cache.test.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheImpl;
import io.quarkus.test.QuarkusUnitTest;

public class CacheConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot(
            jar -> jar.addClass(TestResource.class).addAsResource("cache-config-test.properties", "application.properties"));

    private static final String CACHE_NAME = "test-cache";

    @Inject
    CacheManager cacheManager;

    @Test
    public void testConfig() {
        CaffeineCacheImpl cache = (CaffeineCacheImpl) cacheManager.getCache(CACHE_NAME).get();
        assertEquals(10, cache.getCacheInfo().initialCapacity);
        assertEquals(100L, cache.getCacheInfo().maximumSize);
        assertEquals(Duration.ofSeconds(30L), cache.getCacheInfo().expireAfterWrite);
        assertEquals(Duration.ofDays(2L), cache.getCacheInfo().expireAfterAccess);
        assertTrue(cache.getCacheInfo().metricsEnabled);
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
