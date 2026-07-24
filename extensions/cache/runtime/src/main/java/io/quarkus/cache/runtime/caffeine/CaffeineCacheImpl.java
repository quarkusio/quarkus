package io.quarkus.cache.runtime.caffeine;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
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
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.internal.ContextInternal;

/**
 * This class is an internal Quarkus cache implementation using Caffeine. Do not use it explicitly from your Quarkus
 * application.
 * The public methods signatures may change without prior notice.
 */
public class CaffeineCacheImpl extends AbstractCache implements CaffeineCache {

    private static final Logger LOGGER = Logger.getLogger(CaffeineCacheImpl.class);

    final AsyncCache<Object, Object> cache;

    private final CaffeineCacheInfo cacheInfo;
    private final StatsCounter statsCounter;
    private final boolean recordStats;
    private final boolean expireAfterVariable;

    public CaffeineCacheImpl(CaffeineCacheInfo cacheInfo, boolean recordStats) {
        this.cacheInfo = cacheInfo;
        this.expireAfterVariable = cacheInfo.expireAfterVariable;
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        if (cacheInfo.initialCapacity != null) {
            builder.initialCapacity(cacheInfo.initialCapacity);
        }
        if (cacheInfo.maximumSize != null) {
            builder.maximumSize(cacheInfo.maximumSize);
        }
        if (expireAfterVariable) {
            builder.expireAfter(new Expiry<Object, Object>() {
                @Override
                public long expireAfterCreate(Object key, Object value, long currentTime) {
                    return resolveExpireAfterWriteNanos(value);
                }

                @Override
                public long expireAfterUpdate(Object key, Object value, long currentTime, long currentDuration) {
                    return resolveExpireAfterWriteNanos(value);
                }

                @Override
                public long expireAfterRead(Object key, Object value, long currentTime, long currentDuration) {
                    if (cacheInfo.expireAfterAccess != null) {
                        return cacheInfo.expireAfterAccess.toNanos();
                    }
                    return currentDuration;
                }
            });
        } else {
            if (cacheInfo.expireAfterWrite != null) {
                builder.expireAfterWrite(cacheInfo.expireAfterWrite);
            }
            if (cacheInfo.expireAfterAccess != null) {
                builder.expireAfterAccess(cacheInfo.expireAfterAccess);
            }
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

    private long resolveExpireAfterWriteNanos(Object value) {
        Duration expiresAfter = ExpirableValue.expiresAfterOf(value);
        if (expiresAfter != null) {
            if (expiresAfter.isZero() || expiresAfter.isNegative()) {
                return 0L;
            }
            return expiresAfter.toNanos();
        }
        if (cacheInfo.expireAfterWrite != null) {
            return cacheInfo.expireAfterWrite.toNanos();
        }
        return Long.MAX_VALUE;
    }

    private void ensureVariableExpirySupported() {
        if (!expireAfterVariable) {
            throw new IllegalStateException(
                    "Per-entry expiry requires quarkus.cache.caffeine.\"" + cacheInfo.name
                            + "\".expire-after-variable=true (or the matching default caffeine config)");
        }
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
    public <K, V> Uni<V> get(K key, Function<K, V> valueLoader, Duration expiresAfter) {
        Objects.requireNonNull(key, NULL_KEYS_NOT_SUPPORTED_MSG);
        if (expiresAfter != null) {
            ensureVariableExpirySupported();
        }
        return Uni.createFrom().completionStage(
                /*
                 * Even if CompletionStage is eager, the Supplier used below guarantees that the cache value computation will be
                 * delayed until subscription time. In other words, the cache value computation is done lazily.
                 */
                new Supplier<CompletionStage<V>>() {
                    @Override
                    public CompletionStage<V> get() {
                        CompletionStage<Object> caffeineValue = getFromCaffeine(key, valueLoader, expiresAfter);
                        return cast(caffeineValue);
                    }
                });
    }

    @Override
    public <K, V> Uni<V> getAsync(K key, Function<K, Uni<V>> valueLoader) {
        return getAsync(key, valueLoader, null);
    }

    @Override
    public <K, V> Uni<V> getAsync(K key, Function<K, Uni<V>> valueLoader, Duration expiresAfter) {
        Objects.requireNonNull(key, NULL_KEYS_NOT_SUPPORTED_MSG);
        if (expiresAfter != null) {
            ensureVariableExpirySupported();
        }
        Context context = Vertx.currentContext();
        return Uni.createFrom().context(new Function<io.smallrye.mutiny.Context, Uni<? extends V>>() {
            @Override
            public Uni<? extends V> apply(io.smallrye.mutiny.Context mutinyContext) {
                // When stats are enabled we need to call statsCounter.recordHits(1)/statsCounter.recordMisses(1) accordingly
                StatsRecorder recorder = recordStats ? new OperationalStatsRecorder() : NoopStatsRecorder.INSTANCE;
                @SuppressWarnings("unchecked")
                CompletionStage<V> result = (CompletionStage<V>) cache.asMap().computeIfAbsent(key,
                        new Function<Object, CompletableFuture<Object>>() {
                            @Override
                            @SuppressWarnings("unchecked")
                            public CompletableFuture<Object> apply(Object key) {
                                recorder.onValueAbsent();
                                return valueLoader.apply((K) key)
                                        .map(TO_CACHE_VALUE)
                                        .map(new Function<Object, Object>() {
                                            @Override
                                            public Object apply(Object value) {
                                                return toStoredValue(value, expiresAfter);
                                            }
                                        })
                                        .subscribeAsCompletionStage(mutinyContext);
                            }
                        });
                recorder.doRecord(key);
                return Uni.createFrom().completionStage(result);
            }
        })
                .map(fromCacheValue())
                .emitOn(new Executor() {
                    // We need make sure we go back to the original context when the cache value is computed.
                    // Otherwise, we would always emit on the context having computed the value, which could
                    // break the duplicated context isolation.
                    @Override
                    public void execute(Runnable command) {
                        Context ctx = Vertx.currentContext();
                        if (context == null) {
                            // We didn't capture a context
                            if (ctx == null) {
                                // We are not on a context => we can execute immediately.
                                command.run();
                            } else {
                                // We are on a context.
                                // We cannot continue on the current context as we may share a duplicated context.
                                // We need a new one. Note that duplicate() does not duplicate the duplicated context,
                                // but the root context.
                                ((ContextInternal) ctx).duplicate()
                                        .runOnContext(new Handler<Void>() {
                                            @Override
                                            public void handle(Void ignored) {
                                                command.run();
                                            }
                                        });
                            }
                        } else {
                            // We captured a context.
                            if (ctx == context) {
                                // We are on the same context => we can execute immediately
                                command.run();
                            } else {
                                // 1) We are not on a context (ctx == null) => we need to switch to the captured context.
                                // 2) We are on a different context (ctx != null) => we need to switch to the captured context.
                                context.runOnContext(new Handler<Void>() {
                                    @Override
                                    public void handle(Void ignored) {
                                        command.run();
                                    }
                                });
                            }
                        }
                    }
                });
    }

    @Override
    public <V> CompletableFuture<V> getIfPresent(Object key) {
        Objects.requireNonNull(key, NULL_KEYS_NOT_SUPPORTED_MSG);
        CompletableFuture<Object> existingCacheValue = cache.getIfPresent(key);

        if (existingCacheValue == null) {
            return null;
        } else {
            LOGGER.tracef("Key [%s] found in cache [%s]", key, cacheInfo.name);

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
    private <K, V> CompletableFuture<Object> getFromCaffeine(K key, Function<K, V> valueLoader, Duration expiresAfter) {
        CompletableFuture<Object> newCacheValue = new CompletableFuture<>();
        CompletableFuture<Object> existingCacheValue = cache.asMap().putIfAbsent(key, newCacheValue);
        if (existingCacheValue == null) {
            statsCounter.recordMisses(1);
            try {
                Object value = valueLoader.apply(key);
                newCacheValue.complete(toStoredValue(NullValueConverter.toCacheValue(value), expiresAfter));
            } catch (Throwable t) {
                cache.asMap().remove(key, newCacheValue);
                newCacheValue.complete(toStoredValue(new CaffeineComputationThrowable(t), null));
            }
            return unwrapCacheValueOrThrowable(newCacheValue);
        } else {
            LOGGER.tracef("Key [%s] found in cache [%s]", key, cacheInfo.name);
            statsCounter.recordHits(1);
            return unwrapCacheValueOrThrowable(existingCacheValue);
        }
    }

    private Object toStoredValue(Object cacheData, Duration expiresAfter) {
        if (expireAfterVariable) {
            return ExpirableValue.wrap(cacheData, expiresAfter);
        }
        return cacheData;
    }

    private CompletableFuture<Object> unwrapCacheValueOrThrowable(CompletableFuture<Object> cacheValue) {
        return cacheValue.thenApply(new Function<>() {
            @Override
            public Object apply(Object value) {
                Object data = ExpirableValue.unwrapData(value);
                // If there's a throwable encapsulated into a CaffeineComputationThrowable, it must be rethrown.
                if (data instanceof CaffeineComputationThrowable) {
                    Throwable cause = ((CaffeineComputationThrowable) data).getCause();
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    } else {
                        throw new CacheException(cause);
                    }
                } else {
                    return NullValueConverter.fromCacheValue(data);
                }
            }
        });
    }

    @Override
    public Uni<Void> invalidate(Object key) {
        Objects.requireNonNull(key, NULL_KEYS_NOT_SUPPORTED_MSG);
        return Uni.createFrom().item(new Supplier<>() {
            @Override
            public Void get() {
                cache.synchronous().invalidate(key);
                return null;
            }
        });
    }

    @Override
    public Uni<Void> invalidateAll() {
        return Uni.createFrom().item(new Supplier<>() {
            @Override
            public Void get() {
                cache.synchronous().invalidateAll();
                return null;
            }
        });
    }

    @Override
    public Uni<Void> invalidateIf(Predicate<Object> predicate) {
        return Uni.createFrom().item(new Supplier<>() {
            @Override
            public Void get() {
                cache.asMap().keySet().removeIf(predicate);
                return null;
            }
        });
    }

    @Override
    public Set<Object> keySet() {
        return Set.copyOf(cache.asMap().keySet());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> void put(Object key, CompletableFuture<V> valueFuture) {
        put(key, valueFuture, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> void put(Object key, CompletableFuture<V> valueFuture, Duration expiresAfter) {
        Objects.requireNonNull(key, NULL_KEYS_NOT_SUPPORTED_MSG);
        Objects.requireNonNull(valueFuture, "valueFuture");
        if (expiresAfter != null) {
            ensureVariableExpirySupported();
        }
        if (expireAfterVariable) {
            cache.put(key, valueFuture.thenApply(new Function<V, Object>() {
                @Override
                public Object apply(V value) {
                    return toStoredValue(NullValueConverter.toCacheValue(value), expiresAfter);
                }
            }));
        } else {
            cache.put(key, (CompletableFuture<Object>) valueFuture);
        }
    }

    @Override
    public void setExpireAfterWrite(Duration duration) {
        if (expireAfterVariable) {
            cacheInfo.expireAfterWrite = duration;
            return;
        }
        Optional<FixedExpiration<Object, Object>> fixedExpiration = cache.synchronous().policy().expireAfterWrite();
        if (fixedExpiration.isPresent()) {
            fixedExpiration.get().setExpiresAfter(duration);
            cacheInfo.expireAfterWrite = duration;
        } else {
            throw new IllegalStateException("The write-based expiration policy can only be changed if the cache was " +
                    "constructed with an expire-after-write configuration value");
        }
    }

    @Override
    public void setExpireAfterAccess(Duration duration) {
        if (expireAfterVariable) {
            cacheInfo.expireAfterAccess = duration;
            return;
        }
        Optional<FixedExpiration<Object, Object>> fixedExpiration = cache.synchronous().policy().expireAfterAccess();
        if (fixedExpiration.isPresent()) {
            fixedExpiration.get().setExpiresAfter(duration);
            cacheInfo.expireAfterAccess = duration;
        } else {
            throw new IllegalStateException("The access-based expiration policy can only be changed if the cache was " +
                    "constructed with an expire-after-access configuration value");
        }
    }

    @Override
    public void setMaximumSize(long maximumSize) {
        Optional<Policy.Eviction<Object, Object>> eviction = cache.synchronous().policy().eviction();
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
            return NullValueConverter.fromCacheValue(ExpirableValue.unwrapData(value));
        }
    };

    private static final Function<Object, Object> TO_CACHE_VALUE = new Function<Object, Object>() {

        @Override
        public Object apply(Object value) {
            return NullValueConverter.toCacheValue(value);
        }
    };

}
