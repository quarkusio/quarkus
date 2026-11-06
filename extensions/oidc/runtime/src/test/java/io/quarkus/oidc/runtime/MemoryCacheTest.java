package io.quarkus.oidc.runtime;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

        // replacing an entry does not evict anything
        cache.add("2", new Bean("2b"));
        assertEquals(2, cache.getCacheSize());
        assertEquals("2b", cache.get("2").name);

        // the eldest entry is evicted when the cache is full
        cache.add("3", new Bean("3"));
        assertEquals(2, cache.getCacheSize());

        assertNull(cache.get("1"));
        assertEquals("2b", cache.get("2").name);
        assertEquals("3", cache.get("3").name);

        // the replaced entry is younger than the entries added before it but older than the entries added after it
        cache.add("4", new Bean("4"));
        assertEquals(2, cache.getCacheSize());
        assertNull(cache.get("2"));
        assertEquals("3", cache.get("3").name);
        assertEquals("4", cache.get("4").name);
    }

    @Test
    public void testEvictionFollowsInsertionOrderWithinSameMillisecond() {
        MemoryCache<Bean> cache = new MemoryCache<Bean>(null, Optional.empty(), Duration.ofMinutes(10), 2);
        // the entries are added within the same millisecond, the insertion order must still decide what is evicted
        for (int i = 0; i < 1000; i++) {
            cache.clearCache();
            cache.add("a" + i, new Bean("a"));
            cache.add("b" + i, new Bean("b"));
            cache.add("c" + i, new Bean("c"));
            assertEquals(2, cache.getCacheSize());
            assertNull(cache.get("a" + i));
            assertNotNull(cache.get("b" + i));
            assertNotNull(cache.get("c" + i));
        }
    }

    @Test
    public void testRemoveKeepsInsertionOrder() {
        MemoryCache<Bean> cache = new MemoryCache<Bean>(null, Optional.empty(), Duration.ofMinutes(10), 2);
        cache.add("1", new Bean("1"));
        cache.add("2", new Bean("2"));
        assertEquals("1", cache.remove("1").name);
        assertNull(cache.remove("1"));
        cache.add("3", new Bean("3"));
        assertEquals(2, cache.getCacheSize());

        // "2" is the eldest entry now
        cache.add("4", new Bean("4"));
        assertEquals(2, cache.getCacheSize());
        assertNull(cache.get("2"));
        assertEquals("3", cache.get("3").name);
        assertEquals("4", cache.get("4").name);
    }

    @Test
    public void testDeferredValues() {
        MemoryCache<Bean> cache = new MemoryCache<Bean>(null, Optional.empty(), Duration.ofMinutes(10), 2);

        Bean one = cache.getOrComputeDeferredValue("1", action -> new Bean("1"));
        // the value is computed only once
        assertSame(one, cache.getOrComputeDeferredValue("1", action -> new Bean("1b")));
        cache.getOrComputeDeferredValue("2", action -> new Bean("2"));
        cache.getOrComputeDeferredValue("3", action -> new Bean("3"));
        assertEquals(2, cache.getCacheSize());
        assertNull(cache.get("1"));
        assertEquals("2", cache.get("2").name);
        assertEquals("3", cache.get("3").name);

        // the deferred value can remove itself
        MemoryCache.CacheEntryAction[] actions = new MemoryCache.CacheEntryAction[1];
        cache.getOrComputeDeferredValue("4", action -> {
            actions[0] = action;
            return new Bean("4");
        });
        assertEquals(2, cache.getCacheSize());
        assertNull(cache.get("2"));
        actions[0].remove();
        assertNull(cache.get("4"));
        assertEquals(1, cache.getCacheSize());

        // starting the caching period keeps the entry and its position in the insertion order
        cache.getOrComputeDeferredValue("5", action -> {
            actions[0] = action;
            return new Bean("5");
        });
        actions[0].startCachingPeriod();
        assertEquals("5", cache.get("5").name);
        assertEquals(2, cache.getCacheSize());
        cache.add("6", new Bean("6"));
        assertEquals(2, cache.getCacheSize());
        assertNull(cache.get("3"));
        assertEquals("5", cache.get("5").name);
        assertEquals("6", cache.get("6").name);
    }

    @Test
    public void testConcurrentAdditionsRespectMaxCacheSize() throws Exception {
        int cacheSize = 100;
        int threads = 8;
        int additionsPerThread = 5000;
        MemoryCache<Bean> cache = new MemoryCache<Bean>(null, Optional.empty(), Duration.ofMinutes(10), cacheSize);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                final String prefix = t + "-";
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int i = 0; i < additionsPerThread; i++) {
                        cache.add(prefix + i, new Bean(prefix + i));
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }
        // the size of the cache map is approximate while it is updated concurrently, so concurrent additions
        // can evict a few entries too many or leave a few entries too many behind
        assertTrue(cache.getCacheSize() <= cacheSize + threads, "Cache size: " + cache.getCacheSize());
        assertTrue(cache.getCacheSize() >= cacheSize - 2 * threads, "Cache size: " + cache.getCacheSize());

        // the cache settles at its maximum size as soon as the additions are not concurrent
        for (int i = 0; i < cacheSize; i++) {
            cache.add("final-" + i, new Bean("final-" + i));
        }
        assertEquals(cacheSize, cache.getCacheSize());
        for (int i = 0; i < cacheSize; i++) {
            assertNotNull(cache.get("final-" + i));
        }
    }

    static class Bean {
        String name;

        Bean(String name) {
            this.name = name;
        }
    }
}
