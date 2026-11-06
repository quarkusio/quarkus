package io.quarkus.oidc.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class MemoryCache<T> {
    private volatile Long timerId = null;

    private final Map<String, CacheEntry<T>> cacheMap = new ConcurrentHashMap<>();
    private final ConcurrentSkipListMap<Long, String> insertionOrder = new ConcurrentSkipListMap<>();
    private final AtomicLong sequence = new AtomicLong();
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
            CacheEntry<T> entry = new CacheEntry<>(result, now(), nextSequence());
            CacheEntry<T> previous = cacheMap.put(key, entry);
            if (previous != null) {
                insertionOrder.remove(previous.sequence());
            }
            indexEntry(key, entry);
            evictIfFull();
        }
    }

    private void startTimerIfNotRunning() {
        if (!isTimerRunning() && startTimer != null) {
            startTimer.run();
        }
    }

    public T remove(String key) {
        CacheEntry<T> entry = cacheMap.remove(key);
        if (entry == null) {
            return null;
        }
        insertionOrder.remove(entry.sequence());
        return entry.result();
    }

    public T get(String key) {
        CacheEntry<T> entry = cacheMap.get(key);
        return entry == null ? null : entry.result();
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
            insertionOrder.remove(entry.sequence());
        }
    }

    private void indexEntry(String key, CacheEntry<T> entry) {
        insertionOrder.put(entry.sequence(), key);
        // A concurrent removal or replacement of the entry could not see its position in the insertion order yet,
        // the position must not be left behind in such a case
        CacheEntry<T> current = cacheMap.get(key);
        if (current == null || current.sequence() != entry.sequence()) {
            insertionOrder.remove(entry.sequence());
        }
    }

    private void evictIfFull() {
        while (cacheMap.size() > cacheSize) {
            Map.Entry<Long, String> eldest = insertionOrder.pollFirstEntry();
            if (eldest == null) {
                return;
            }
            // a stale position is skipped and the next eldest entry is evicted instead
            evict(eldest.getValue(), eldest.getKey());
        }
    }

    private void evict(String key, long sequence) {
        // The entry is removed only if it still has the polled sequence. It is done atomically because the entry
        // can be replaced with a new one having the same sequence when its caching period is restarted.
        cacheMap.computeIfPresent(key, (k, entry) -> entry.sequence() == sequence ? null : entry);
    }

    private boolean isEntryExpired(CacheEntry<T> entry, long now) {
        return entry.createdTime + cacheTimeToLive.toMillis() < now;
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private long nextSequence() {
        return sequence.incrementAndGet();
    }

    public int getCacheSize() {
        return cacheMap.size();
    }

    public void clearCache() {
        // the entries are removed one by one to keep the insertion order in sync with the cache map
        for (String key : cacheMap.keySet()) {
            remove(key);
        }
    }

    public void stopTimer(Vertx vertx) {
        if (timerId != null && vertx.cancelTimer(timerId)) {
            timerId = null;
        }
    }

    public boolean isTimerRunning() {
        return timerId != null;
    }

    private record CacheEntry<S>(S result, long createdTime, long sequence) {

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
                var createIfAbsent = new Function<String, CacheEntry<T>>() {

                    private boolean applied = false;

                    @Override
                    public CacheEntry<T> apply(String k) {
                        applied = true;
                        long entryCreated = now() + GRACE_PERIOD_MILLIS;
                        return new CacheEntry<>(valueFactory.apply(DeferredCacheValue.this), entryCreated,
                                nextSequence());
                    }
                };
                cacheEntry = cacheMap.computeIfAbsent(key, createIfAbsent);
                if (createIfAbsent.applied) {
                    indexEntry(key, cacheEntry);
                    startTimerIfNotRunning();
                    evictIfFull();
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
                // the sequence is kept so that the position in the insertion order does not change
                var newEntry = cacheEntry = new CacheEntry<>(oldEntry.result(), now(), oldEntry.sequence());
                cacheMap.replace(key, oldEntry, newEntry);
            }
        }

    }

    interface CacheEntryAction {

        void remove();

        void startCachingPeriod();

    }
}
