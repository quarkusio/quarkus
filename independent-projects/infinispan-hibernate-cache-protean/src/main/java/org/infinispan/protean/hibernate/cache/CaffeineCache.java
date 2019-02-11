package org.infinispan.protean.hibernate.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Ticker;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class CaffeineCache implements InternalCache {

    private static final Logger log = Logger.getLogger(CaffeineCache.class);
    private static final boolean trace = log.isTraceEnabled();

    private static final Ticker TICKER = Ticker.systemTicker();
    static final Time.NanosService TIME_SERVICE = TICKER::read;

    private final Cache cache;
    private final String cacheName;

    CaffeineCache(String cacheName, InternalCacheConfig config, Time.NanosService nanosTimeService) {
        Duration maxIdle = config.maxIdle;
        long objectCount = config.objectCount;

        this.cacheName = cacheName;
        final Caffeine cacheBuilder = Caffeine.newBuilder()
                .ticker(nanosTimeService::nanoTime);

        if (!Time.isForever(maxIdle)) {
            cacheBuilder.expireAfter(new CacheExpiryPolicy(maxIdle));
        }

        if (objectCount >= 0) {
            cacheBuilder.maximumSize(objectCount);
        }

        this.cache = cacheBuilder.build();
    }

    @Override
    public Object getOrNull(Object key) {
        final Object value = cache.getIfPresent(key);
        if (trace) {
            log.tracef("Cache get(key=%s) returns: %s", key, value);
        }

        return value;
    }

    @Override
    public void putIfAbsent(Object key, Object value) {
        if (trace) {
            log.tracef("Cache put if absent key=%s value=%s", key, value);
        }

        cache.asMap().putIfAbsent(key, value);
    }

    @Override
    public void put(Object key, Object value) {
        if (trace) {
            log.tracef("Cache put key=%s value=%s", key, value);
        }

        cache.put(key, value);
    }

    @Override
    public Object compute(Object key, BiFunction<Object, Object, Object> remappingFunction) {
        final Object result = cache.asMap().compute(key, remappingFunction);
        if (trace) {
            log.tracef("Computing function=%s on key=%s returns: %s", remappingFunction, key, result);
        }

        return result;
    }

    @Override
    public void invalidate(Object key) {
        if (trace) {
            log.tracef("Cache invalidate key %s", key);
        }

        cache.invalidate(key);
    }

    @Override
    public long size(Predicate<Map.Entry> filter) {
        // Size calculated for stats, so try to get as accurate count as possible
        // by performing any cleanup operations before returning the result
        cache.cleanUp();

        if (filter == null) {
            final long size = cache.estimatedSize();
            if (trace) {
                log.tracef("Cache(%s) size estimated at %d elements", cacheName, size);
            }

            return size;
        } else {
            final long size = cache.asMap().entrySet().stream().filter(filter).count();
            if (trace) {
                log.tracef("Cache(%s) size for entries matching filter(%s) is %d elements", cacheName, filter, size);
            }

            return size;
        }
    }

    @Override
    public void forEach(Predicate<Map.Entry> filter, Consumer<Map.Entry> action) {
        cache.asMap().entrySet().stream().filter(filter).forEach(action);
    }

    @Override
    public void stop() {
        if (trace) {
            log.tracef("Cleanup cache %s", cacheName);
        }

        cache.cleanUp();
    }

    @Override
    public void invalidateAll() {
        if (trace) {
            log.tracef("Invalidate all in cache %s", cacheName);
        }

        cache.invalidateAll();
    }

    // Unnecessarily checks if values are VersionedEntry if non-strict not used.
    // However, with caches kept at region level so, impossible to know at cache creation whether non-strict will be used or not.
    // Alternative would be for cache granularity to be at data access level (needs more analysis).
    // e.g. regions would then need to aggregate counts for all data accesses for that region.
    private static final class CacheExpiryPolicy implements Expiry {

        private final long maxIdleNanos;

        public CacheExpiryPolicy(Duration maxIdle) {
            if (maxIdle == null)
                this.maxIdleNanos = -1;
            else
                this.maxIdleNanos = maxIdle.toNanos();
        }

        @Override
        public long expireAfterCreate(Object key, Object value, long currentTime) {
            return calculateExpiration(value, currentTime);
        }

        private long calculateExpiration(Object value, long currentTime) {
            if (value instanceof VersionedEntry) {
                // Deal with versioned entry expirations
                final VersionedEntry versioned = (VersionedEntry) value;
                if (maxIdleNanos > 0) {
                    final long idleDeadline = currentTime + maxIdleNanos;
                    final long versionedLifespan = versioned.getLifespanNanos();
                    log.tracef("Expire after create, either idle deadline %d (ns) or versioned entry lifespan %d (ns)", idleDeadline, versionedLifespan);
                    return Math.min(idleDeadline, versionedLifespan);
                }

                return versioned.getLifespanNanos();
            }

            if (maxIdleNanos > 0)
                return maxIdleNanos;

            return Long.MAX_VALUE;
        }

        @Override
        public long expireAfterUpdate(Object key, Object value, long currentTime, long currentDuration) {
            return calculateExpiration(value, currentTime);
        }

        @Override
        public long expireAfterRead(Object key, Object value, long currentTime, long currentDuration) {
            if (maxIdleNanos > 0) {
                return maxIdleNanos;
            }

            return Long.MAX_VALUE;
        }

    }

}
