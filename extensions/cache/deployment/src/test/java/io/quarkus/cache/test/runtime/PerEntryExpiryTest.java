package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.test.QuarkusExtensionTest;

public class PerEntryExpiryTest {

    private static final String VARIABLE_CACHE = "variable-expiry-cache";
    private static final String FIXED_CACHE = "fixed-expiry-cache";

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClass(CachedService.class)
                    .addAsResource(new StringAsset(
                            "quarkus.cache.caffeine.\"" + VARIABLE_CACHE + "\".expire-after-variable=true\n"),
                            "application.properties"));

    @CacheName(VARIABLE_CACHE)
    Cache variableCache;

    @CacheName(FIXED_CACHE)
    Cache fixedCache;

    @Test
    public void testPerEntryExpiry() throws InterruptedException {
        AtomicInteger calls = new AtomicInteger();
        assertEquals("v1", variableCache.get("k1", k -> {
            calls.incrementAndGet();
            return "v1";
        }, Duration.ofMillis(200)).await().indefinitely());
        assertEquals(1, calls.get());

        assertEquals("v1", variableCache.get("k1", k -> {
            calls.incrementAndGet();
            return "other";
        }, Duration.ofMinutes(1)).await().indefinitely());
        assertEquals(1, calls.get());

        Thread.sleep(400);

        assertEquals("v2", variableCache.get("k1", k -> {
            calls.incrementAndGet();
            return "v2";
        }, Duration.ofMinutes(1)).await().indefinitely());
        assertEquals(2, calls.get());
    }

    @Test
    public void testPutWithExpiry() throws InterruptedException {
        CaffeineCache caffeineCache = variableCache.as(CaffeineCache.class);
        caffeineCache.put("put-key", CompletableFuture.completedFuture("put-value"), Duration.ofMillis(200));
        assertEquals("put-value", caffeineCache.getIfPresent("put-key").join());
        Thread.sleep(400);
        assertNull(caffeineCache.getIfPresent("put-key"));
    }

    @Test
    public void testVariableExpiryRequired() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> fixedCache.get("k", k -> "v", Duration.ofSeconds(1)).await().indefinitely());
        assertTrue(exception.getMessage().contains("expire-after-variable"));
    }

    @Singleton
    static class CachedService {

        @CacheResult(cacheName = VARIABLE_CACHE)
        public String cachedMethod(String key) {
            return key;
        }
    }
}
