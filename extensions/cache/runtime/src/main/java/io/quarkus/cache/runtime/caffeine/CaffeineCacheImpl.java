package io.quarkus.cache.runtime.caffeine;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Policy;
import com.github.benmanes.caffeine.cache.Policy.FixedExpiration;
import com.github.benmanes.caffeine.cache.stats.ConcurrentStatsCounter;
import com.github.benmanes.caffeine.cache.stats.StatsCounter;

import io.quarkus.cache.CacheException;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.cache.runtime.AbstractCache;
import io.quarkus.cache.runtime.NullValueConverter;
import io.smallrye.mutiny.Uni;

/**
 * This class is an internal Quarkus cache implementation using Caffeine. Do not use it explicitly from your Quarkus
 * application.
 * The public methods signatures may change without prior notice.
 */
public class CaffeineCacheImpl extends AbstractCache implements CaffeineCache {

    private static final Logger LOGGER = Logger.getLogger(CaffeineCacheImpl.class);

    final AsyncCache<Object, CacheValue<Object>> cache;

    private final CaffeineCacheInfo cacheInfo;
    private final StatsCounter statsCounter;
    private final boolean recordStats;


    public CaffeineCacheImpl(CaffeineCacheInfo cacheInfo, boolean recordStats) {
        this.cacheInfo = cacheInfo;
        Caffeine<Object, CacheValue<Object>> builder = (Caffeine) Caffeine.newBuilder();
        builder.expireAfter(new Expiry<Object, CacheValue<Object>>() {
            @Override
            public long expireAfterCreate(Object key, CacheValue<Object> value, long currentTime) {
                return value.expiresIn != null ? value.expiresIn.toNanos()
                        : cacheInfo.expireAfterWrite != null ? cacheInfo.expireAfterWrite.toNanos() : Long.MAX_VALUE;
            }

            @Override
            public long expireAfterUpdate(Object key, CacheValue<Object> value, long currentTime, long currentDuration) {
                return value.expiresIn != null ? value.expiresIn.toNanos()
                        : cacheInfo.expireAfterWrite != null ? cacheInfo.expireAfterWrite.toNanos() : Long.MAX_VALUE;
            }

            @Override
            public long expireAfterRead(Object key, CacheValue<Object> value, long currentTime, long currentDuration) {
                return cacheInfo.expireAfterAccess != null ? cacheInfo.expireAfterAccess.toNanos() : Long.MAX_VALUE;
            }
        });
        if (cacheInfo.initialCapacity != null) {
            builder.initialCapacity(cacheInfo.initialCapacity);
        }
        if (cacheInfo.maximumSize != null) {
            builder.maximumSize(cacheInfo.maximumSize);
        }

        this.recordStats = recordStats;
        if (recordStats) {
            LOGGER.tracef("Recording Caffeine stats for cache [%s]", cacheInfo.name);
            statsCounter = new ConcurrentStatsCounter();
            builder.recordStats(new Supplier<StatsCounter>() {
                @Override
                public StatsCounter get() {
                    return statsCounter;
                }
            });
        } else {
            LOGGER.tracef("Caffeine stats recording is disabled for cache [%s]", cacheInfo.name);
            statsCounter = StatsCounter.disabledStatsCounter();
        }
        cache = builder.buildAsync();
    }

    @Override
    public String getName() {
        return cacheInfo.name;
    }

    @Override
    public <K, V> Uni<V> get(K key, Function<K, V> valueLoader) {
        return get(key, valueLoader, null);
    }

    @Override
    public <K, V> Uni<V> get(K key, Function<K, V> valueLoader, Duration expiresIn) {
        Objects.requireNonNull(key, NULL_KEYS_NOT_SUPPORTED_MSG);
        return Uni.createFrom().completionStage(
                /*
                 * Even if CompletionStage is eager, the Supplier used below guarantees that the cache value computation will be
                 * delayed until subscription time. In other words, the cache value computation is done lazily.
                 */
                new Supplier<CompletionStage<V>>() {
                    @Override
                    public CompletionStage<V> get() {
                        CompletionStage<Object> caffeineValue = getFromCaffeine(key, (k) -> CacheValue.builder()
                                .data(valueLoader.apply(k))
                                .expiresIn(expiresIn)
                                .build());
                        return cast(caffeineValue);
                    }
                });
    }

    @Override
    public <K, V> Uni<V> getAsync(K key, Function<K, Uni<V>> valueLoader) {
        return getAsync(key, valueLoader, null);
    }

    @Override
    public <K, V> Uni<V> getAsync(K key, Function<K, Uni<V>> valueLoader, Duration expiresIn) {
        Objects.requireNonNull(key, NULL_KEYS_NOT_SUPPORTED_MSG);
        return Uni.createFrom()
                .completionStage(new Supplier<CompletionStage<V>>() {
                    @Override
                    public CompletionStage<V> get() {
                        // When stats are enabled we need to call statsCounter.recordHits(1)/statsCounter.recordMisses(1) accordingly
                        StatsRecorder recorder = recordStats ? new OperationalStatsRecorder() : NoopStatsRecorder.INSTANCE;
                        @SuppressWarnings("unchecked")
                        CompletionStage<V> result = (CompletionStage<V>) cache.asMap().computeIfAbsent(key,
                                new Function<Object, CompletableFuture<CacheValue<Object>>>() {
                                    @Override
                                    public CompletableFuture<CacheValue<Object>> apply(Object key) {
                                        recorder.onValueAbsent();
                                        return valueLoader.apply((K) key)
                                                .map(TO_CACHE_VALUE)
                                                .map(x -> CacheValue.builder()
                                                        .data(x)
                                                        .expiresIn(expiresIn)
                                                        .build())
                                                .subscribeAsCompletionStage();
                                    }
                                }).thenApply(CacheValue::getData);
                        recorder.doRecord(key);
                        return result;
                    }
                }).map(fromCacheValue());
    }

    @Override
    public <V> CompletableFuture<V> getIfPresent(Object key) {
        Objects.requireNonNull(key, NULL_KEYS_NOT_SUPPORTED_MSG);
        CompletableFuture<CacheValue<Object>> existingCacheValue = cache.getIfPresent(key);

        // record metrics, if not null apply casting
        if (existingCacheValue == null) {
            statsCounter.recordMisses(1);
            return null;
        } else {
            LOGGER.tracef("Key [%s] found in cache [%s]", key, cacheInfo.name);
            statsCounter.recordHits(1);

            // cast, but still throw the CacheException in case it fails
            return unwrapCacheValueOrThrowable(existingCacheValue)
                    .thenApply(new Function<>() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public V apply(Object value) {
                            try {
                                return (V) value;
                            } catch (ClassCastException e) {
                                throw new CacheException("An existing cached value type does not match the requested type", e);
                            }
                        }
                    });

        }
    }

    /**
     * Returns a {@link CompletableFuture} holding the cache value identified by {@code key}, obtaining that value from
     * {@code valueLoader} if necessary. The value computation is done synchronously on the calling thread and the
     * {@link CompletableFuture} is immediately completed before being returned.
     *
     * @param key cache key
     * @param valueLoader function used to compute the cache value if {@code key} is not already associated with a value
     * @return a {@link CompletableFuture} holding the cache value
     * @throws CacheException if an exception is thrown during the cache value computation
     */
    private <K, V> CompletableFuture<Object> getFromCaffeine(K key, Function<K, CacheValue<V>> valueLoader) {
        CompletableFuture<CacheValue<Object>> newCacheValue = new CompletableFuture<>();
        CompletableFuture<CacheValue<Object>> existingCacheValue = cache.asMap().putIfAbsent(key, newCacheValue);
        if (existingCacheValue == null) {
            statsCounter.recordMisses(1);
            try {
                CacheValue<Object> value = (CacheValue<Object>) valueLoader.apply(key);
                newCacheValue.complete(value);
            } catch (Throwable t) {
                cache.asMap().remove(key, newCacheValue);
                newCacheValue.complete(CacheValue.builder()
                        .data(new CaffeineComputationThrowable(t))
                        .build());
            }
            return unwrapCacheValueOrThrowable(newCacheValue);
        } else {
            LOGGER.tracef("Key [%s] found in cache [%s]", key, cacheInfo.name);
            statsCounter.recordHits(1);
            return unwrapCacheValueOrThrowable(existingCacheValue);
        }
    }

    private CompletableFuture<Object> unwrapCacheValueOrThrowable(CompletableFuture<CacheValue<Object>> cacheValue) {
        return cacheValue.thenApply(new Function<>() {
            @Override
            public Object apply(CacheValue<Object> value) {
                // If there's a throwable encapsulated into a CaffeineComputationThrowable, it must be rethrown.
                if (value.getData() instanceof CaffeineComputationThrowable) {
                    Throwable cause = ((CaffeineComputationThrowable) value.getData()).getCause();
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    } else {
                        throw new CacheException(cause);
                    }
                } else {
                    return NullValueConverter.fromCacheValue(value.getData());
                }
            }
        });
    }

    @Override
    public Uni<Void> invalidate(Object key) {
        Objects.requireNonNull(key, NULL_KEYS_NOT_SUPPORTED_MSG);
        return Uni.createFrom().item(new Supplier<Void>() {
            @Override
            public Void get() {
                cache.synchronous().invalidate(key);
                return null;
            }
        });
    }

    @Override
    public Uni<Void> invalidateAll() {
        return Uni.createFrom().item(new Supplier<Void>() {
            @Override
            public Void get() {
                cache.synchronous().invalidateAll();
                return null;
            }
        });
    }

    @Override
    public Uni<Void> invalidateIf(Predicate<Object> predicate) {
        return Uni.createFrom().item(new Supplier<Void>() {
            @Override
            public Void get() {
                cache.asMap().keySet().removeIf(predicate);
                return null;
            }
        });
    }

    @Override
    public Set<Object> keySet() {
        return Collections.unmodifiableSet(new HashSet<>(cache.asMap().keySet()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> void put(Object key, CompletableFuture<V> valueFuture) {
        put(key, valueFuture, null);
    }

    @Override
    public <V> void put(Object key, CompletableFuture<V> valueFuture, Duration expiresIn) {
        cache.put(key, (CompletableFuture<CacheValue<Object>>) valueFuture.thenApply(x -> CacheValue.builder()
                .expiresIn(expiresIn)
                .data(x)
                .build()));
    }

    @Override
    public void setExpireAfterWrite(Duration duration) {
        cacheInfo.expireAfterWrite = duration;
    }

    @Override
    public void setExpireAfterAccess(Duration duration) {
        cacheInfo.expireAfterAccess = duration;
    }

    @Override
    public void setMaximumSize(long maximumSize) {
        Optional<Policy.Eviction<Object, CacheValue<Object>>> eviction = cache.synchronous().policy().eviction();
        if (eviction.isPresent()) {
            eviction.get().setMaximum(maximumSize);
            cacheInfo.maximumSize = maximumSize;
        } else {
            throw new IllegalStateException("The maximum size can only be changed if the cache was constructed with a " +
                    "maximum-size configuration value");
        }
    }

    // For testing purposes only.
    public CaffeineCacheInfo getCacheInfo() {
        return cacheInfo;
    }

    public long getSize() {
        return cache.synchronous().estimatedSize();
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(Object value) {
        try {
            return (T) value;
        } catch (ClassCastException e) {
            throw new CacheException(
                    "An existing cached value type does not match the type returned by the value loading function", e);
        }
    }

    @SuppressWarnings("unchecked")
    private <V> Function<V, V> fromCacheValue() {
        return (Function<V, V>) FROM_CACHE_VALUE;
    }

    private interface StatsRecorder {

        void onValueAbsent();

        <K> void doRecord(K key);

    }

    private static class NoopStatsRecorder implements StatsRecorder {

        static final NoopStatsRecorder INSTANCE = new NoopStatsRecorder();

        @Override
        public void onValueAbsent() {
            // no-op
        }

        @Override
        public <K> void doRecord(K key) {
            // no-op
        }

    }

    private class OperationalStatsRecorder implements StatsRecorder {

        private boolean valueAbsent;

        @Override
        public void onValueAbsent() {
            valueAbsent = true;
        }

        @Override
        public <K> void doRecord(K key) {
            if (valueAbsent) {
                statsCounter.recordMisses(1);
            } else {
                LOGGER.tracef("Key [%s] found in cache [%s]", key, cacheInfo.name);
                statsCounter.recordHits(1);
            }
        }

    }

    private static final Function<Object, Object> FROM_CACHE_VALUE = new Function<Object, Object>() {

        @Override
        public Object apply(Object value) {
            return NullValueConverter.fromCacheValue(value);
        }
    };

    private static final Function<Object, Object> TO_CACHE_VALUE = new Function<Object, Object>() {

        @Override
        public Object apply(Object value) {
            return NullValueConverter.toCacheValue(value);
        }
    };

}
