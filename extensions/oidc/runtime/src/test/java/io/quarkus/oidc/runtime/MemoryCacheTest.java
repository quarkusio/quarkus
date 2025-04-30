package io.quarkus.oidc.runtime;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;

public class MemoryCacheTest {

    static Vertx vertx = Vertx.vertx();

    @AfterAll
    public static void closeVertxClient() {
        if (vertx != null) {
            vertx.close().toCompletionStage().toCompletableFuture().join();
            vertx = null;
        }
    }

    @Test
    public void testCache() throws Exception {

        MemoryCache<Bean> cache = new MemoryCache<Bean>(vertx,
                // timer interval
                Optional.of(Duration.ofSeconds(1)),
                // entry is valid for 3 seconds
                Duration.ofSeconds(2),
                // max cache size
                2);
        cache.add("1", new Bean("1"));
        cache.add("2", new Bean("2"));
        assertEquals(2, cache.getCacheSize());

        assertEquals("1", cache.get("1").name);
        assertEquals("2", cache.get("2").name);

        assertEquals("1", cache.remove("1").name);
        assertNull(cache.get("1"));
        assertEquals("2", cache.get("2").name);
        assertEquals(1, cache.getCacheSize());

        assertTrue(cache.isTimerRunning());

        await().atMost(Duration.ofSeconds(5)).until(new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                return cache.getCacheSize() == 0;
            }

        });

        cache.stopTimer(vertx);
        assertFalse(cache.isTimerRunning());
    }

    @Test
    public void testAddWhenMaxCacheSizeIsReached() throws Exception {

        MemoryCache<Bean> cache = new MemoryCache<Bean>(vertx,
                // timer interval
                Optional.empty(),
                // entry is valid for 3 seconds
                Duration.ofSeconds(3),
                // max cache size
                2);
        assertFalse(cache.isTimerRunning());

        cache.add("1", new Bean("1"));
        cache.add("2", new Bean("2"));
        assertEquals(2, cache.getCacheSize());

        // Currently, if the cache is full and a new entry has to be added, then the whole cache is cleared
        // It can be optimized to remove the oldest entry only in the future

        cache.add("3", new Bean("3"));
        assertEquals(1, cache.getCacheSize());

        assertNull(cache.get("1"));
        assertNull(cache.get("2"));
        assertEquals("3", cache.get("3").name);
    }

    static class Bean {
        String name;

        Bean(String name) {
            this.name = name;
        }
    }
}
