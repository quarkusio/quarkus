package io.quarkus.cache.test.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheImpl;
import io.quarkus.test.QuarkusUnitTest;

public class CacheConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot(
            jar -> jar.addClass(TestResource.class).addAsResource("cache-config-test.properties", "application.properties"));

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
        assertEquals(10, cache.getCacheInfo().initialCapacity);
        assertEquals(100L, cache.getCacheInfo().maximumSize);
        assertEquals(Duration.ofSeconds(30L), cache.getCacheInfo().expireAfterWrite);
        assertEquals(Duration.ofDays(2L), cache.getCacheInfo().expireAfterAccess);
        assertTrue(cache.getCacheInfo().metricsEnabled);

        long newMaxSize = 123L;
        cache.setMaximumSize(newMaxSize);
        assertEquals(newMaxSize, cache.getCacheInfo().maximumSize);

        Duration newExpireAfterWrite = Duration.ofDays(456L);
        cache.setExpireAfterWrite(newExpireAfterWrite);
        assertEquals(newExpireAfterWrite, cache.getCacheInfo().expireAfterWrite);

        Duration newExpireAfterAccess = Duration.ofDays(789L);
        cache.setExpireAfterAccess(newExpireAfterAccess);
        assertEquals(newExpireAfterAccess, cache.getCacheInfo().expireAfterAccess);
    }

    @Test
    void testCache2Config() {
        CaffeineCacheImpl cache = (CaffeineCacheImpl) testCache2;
        assertEquals(80, cache.getCacheInfo().initialCapacity);
        assertNull(cache.getCacheInfo().maximumSize);
        assertEquals(Duration.ofDays(4L), cache.getCacheInfo().expireAfterWrite);
        assertEquals(Duration.ofSeconds(90L), cache.getCacheInfo().expireAfterAccess);
        assertFalse(cache.getCacheInfo().metricsEnabled);
    }

    @Test
    void testCache3Config() {
        CaffeineCacheImpl cache = (CaffeineCacheImpl) testCache3;
        assertEquals(123, cache.getCacheInfo().initialCapacity);
        assertNull(cache.getCacheInfo().maximumSize);
        assertNull(cache.getCacheInfo().expireAfterWrite);
        assertNull(cache.getCacheInfo().expireAfterAccess);
        assertTrue(cache.getCacheInfo().metricsEnabled);
    }

    @Test
    void setMaximumSizeShouldThrowWhenNoInitialConfigValue() {
        assertThrows(IllegalStateException.class, () -> {
            noConfigCache.as(CaffeineCache.class).setMaximumSize(123L);
        });
    }

    @Test
    void setExpireAfterWriteShouldThrowWhenNoInitialConfigValue() {
        assertThrows(IllegalStateException.class, () -> {
            noConfigCache.as(CaffeineCache.class).setExpireAfterWrite(Duration.ofSeconds(456L));
        });
    }

    @Test
    void setExpireAfterAccessShouldThrowWhenNoInitialConfigValue() {
        assertThrows(IllegalStateException.class, () -> {
            noConfigCache.as(CaffeineCache.class).setExpireAfterAccess(Duration.ofSeconds(789L));
        });
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
