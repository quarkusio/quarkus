package io.quarkus.oidc.runtime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class MemoryCache<T> {
    private volatile Long timerId = null;

    private final Map<String, CacheEntry<T>> cacheMap = new ConcurrentHashMap<>();
    private final AtomicInteger size = new AtomicInteger();
    private final Duration cacheTimeToLive;
    private final int cacheSize;
    private final Runnable startTimer;

    public MemoryCache(Vertx vertx, Optional<Duration> cleanUpTimerInterval,
            Duration cacheTimeToLive, int cacheSize) {
        this.cacheTimeToLive = cacheTimeToLive;
        this.cacheSize = cacheSize;
        if (vertx != null && cleanUpTimerInterval.isPresent()) {
            this.startTimer = () -> {
                synchronized (MemoryCache.this) {
                    if (timerId == null) {
                        timerId = vertx.setPeriodic(cleanUpTimerInterval.get().toMillis(), new Handler<Long>() {
                            @Override
                            public void handle(Long event) {
                                if (!cacheMap.isEmpty()) {
                                    // Remove all the entries which have expired
                                    removeInvalidEntries();
                                }
                            }
                        });
                    }
                }
            };
        } else {
            this.startTimer = null;
        }
    }

    public void add(String key, T result) {
        if (cacheSize > 0) {
            startTimerIfNotRunning();
            if (!prepareSpaceForNewCacheEntry()) {
                clearCache();
            }
            cacheMap.put(key, new CacheEntry<>(result, now()));
        }
    }

    private void startTimerIfNotRunning() {
        if (!isTimerRunning() && startTimer != null) {
            startTimer.run();
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

    T getOrComputeDeferredValue(String key, Function<CacheEntryAction, T> itemFactory) {
        return new DeferredCacheValue(key).computeIfAbsent(itemFactory);
    }

    private void removeInvalidEntries() {
        long now = now();
        for (Map.Entry<String, CacheEntry<T>> next : cacheMap.entrySet()) {
            if (next != null) {
                if (isEntryExpired(next.getValue(), now)) {
                    removeCacheEntry(next.getKey(), next.getValue());
                }
            }
        }
    }

    private void removeCacheEntry(String key, CacheEntry<T> entry) {
        if (cacheMap.remove(key, entry)) {
            size.decrementAndGet();
        }
    }

    private void evictEldest() {
        int overflow = cacheMap.size() - cacheSize;
        if (overflow <= 0) {
            return;
        }
        long[] times = new long[overflow];
        var entries = new ArrayList<Map.Entry<String, CacheEntry<T>>>(overflow);
        int found = 0;
        for (var next : cacheMap.entrySet()) {
            long createdTime = next.getValue().createdTime();
            if (found < overflow) {
                times[found] = createdTime;
                entries.add(next);
                found++;
            } else {
                int youngest = 0;
                for (int i = 1; i < overflow; i++) {
                    if (times[i] > times[youngest]) {
                        youngest = i;
                    }
                }
                if (createdTime < times[youngest]) {
                    times[youngest] = createdTime;
                    entries.set(youngest, next);
                }
            }
        }
        entries.forEach(e -> removeCacheEntry(e.getKey(), e.getValue()));
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

    private record CacheEntry<S>(S result, long createdTime) {

    }

    private final class DeferredCacheValue implements CacheEntryAction {

        // give time to resolve the deferred value before we start TTL
        private static final long GRACE_PERIOD_MILLIS = 5_000L;

        private final String key;
        private volatile CacheEntry<T> cacheEntry;

        private DeferredCacheValue(String key) {
            this.key = Objects.requireNonNull(key);
            this.cacheEntry = null;
        }

        private T computeIfAbsent(Function<CacheEntryAction, T> valueFactory) {
            if (cacheSize <= 0) {
                return valueFactory.apply(this);
            }
            if (cacheEntry == null) {
                if (cacheMap.size() >= cacheSize) {
                    removeInvalidEntries();
                }
                var createIfAbsent = new Function<String, CacheEntry<T>>() {

                    private boolean applied = false;

                    @Override
                    public CacheEntry<T> apply(String k) {
                        applied = true;
                        long entryCreated = now() + GRACE_PERIOD_MILLIS;
                        return new CacheEntry<>(valueFactory.apply(DeferredCacheValue.this), entryCreated);
                    }
                };
                cacheEntry = cacheMap.computeIfAbsent(key, createIfAbsent);
                if (createIfAbsent.applied) {
                    startTimerIfNotRunning();
                    size.incrementAndGet();
                    if (cacheMap.size() > cacheSize) {
                        evictEldest();
                    }
                }
            }
            return cacheEntry.result();
        }

        @Override
        public void remove() {
            var e = cacheEntry;
            if (e != null) {
                removeCacheEntry(key, e);
            }
        }

        // must be called after the old entry was created, otherwise it is no-op
        @Override
        public void startCachingPeriod() {
            var oldEntry = cacheEntry;
            if (oldEntry != null) {
                var newEntry = cacheEntry = new CacheEntry<>(oldEntry.result(), now());
                cacheMap.replace(key, oldEntry, newEntry);
            }
        }

    }

    interface CacheEntryAction {

        void remove();

        void startCachingPeriod();

    }
}
