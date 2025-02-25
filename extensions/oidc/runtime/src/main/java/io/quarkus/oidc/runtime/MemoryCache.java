package io.quarkus.oidc.runtime;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class MemoryCache<T> {
    private volatile Long timerId = null;

    private final Map<String, CacheEntry<T>> cacheMap = new ConcurrentHashMap<>();
    private final AtomicInteger size = new AtomicInteger();
    private final Duration cacheTimeToLive;
    private final int cacheSize;

    public MemoryCache(Vertx vertx, Optional<Duration> cleanUpTimerInterval,
            Duration cacheTimeToLive, int cacheSize) {
        this.cacheTimeToLive = cacheTimeToLive;
        this.cacheSize = cacheSize;
        init(vertx, cleanUpTimerInterval);
    }

    private void init(Vertx vertx, Optional<Duration> cleanUpTimerInterval) {
        if (vertx != null && cleanUpTimerInterval.isPresent()) {
            timerId = vertx.setPeriodic(cleanUpTimerInterval.get().toMillis(), new Handler<Long>() {
                @Override
                public void handle(Long event) {
                    // Remove all the entries which have expired
                    removeInvalidEntries();
                }
            });
        }
    }

    public void add(String key, T result) {
        if (cacheSize > 0) {
            if (!prepareSpaceForNewCacheEntry()) {
                clearCache();
            }
            cacheMap.put(key, new CacheEntry<T>(result));
        }
    }

    public T remove(String key) {
        CacheEntry<T> entry = removeCacheEntry(key);
        return entry == null ? null : entry.result;
    }

    public T get(String key) {
        CacheEntry<T> entry = cacheMap.get(key);
        return entry == null ? null : entry.result;
    }

    public boolean containsKey(String key) {
        return cacheMap.containsKey(key);
    }

    private void removeInvalidEntries() {
        long now = now();
        for (Iterator<Map.Entry<String, CacheEntry<T>>> it = cacheMap.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, CacheEntry<T>> next = it.next();
            if (next != null) {
                if (isEntryExpired(next.getValue(), now)) {
                    try {
                        it.remove();
                        size.decrementAndGet();
                    } catch (IllegalStateException ex) {
                        // continue
                    }
                }
            }
        }
    }

    private boolean prepareSpaceForNewCacheEntry() {
        int currentSize;
        do {
            currentSize = size.get();
            if (currentSize == cacheSize) {
                return false;
            }
        } while (!size.compareAndSet(currentSize, currentSize + 1));
        return true;
    }

    private CacheEntry<T> removeCacheEntry(String token) {
        CacheEntry<T> entry = cacheMap.remove(token);
        if (entry != null) {
            size.decrementAndGet();
        }
        return entry;
    }

    private boolean isEntryExpired(CacheEntry<T> entry, long now) {
        return entry.createdTime + cacheTimeToLive.toMillis() < now;
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static class CacheEntry<T> {
        volatile T result;
        long createdTime = System.currentTimeMillis();

        public CacheEntry(T result) {
            this.result = result;
        }
    }

    public int getCacheSize() {
        return cacheMap.size();
    }

    public void clearCache() {
        cacheMap.clear();
        size.set(0);
    }

    public void stopTimer(Vertx vertx) {
        if (timerId != null && vertx.cancelTimer(timerId)) {
            timerId = null;
        }
    }

    public boolean isTimerRunning() {
        return timerId != null;
    }

}
