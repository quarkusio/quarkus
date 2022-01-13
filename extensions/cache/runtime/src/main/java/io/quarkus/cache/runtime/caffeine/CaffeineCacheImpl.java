package io.quarkus.cache.runtime.caffeine;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.ConcurrentStatsCounter;
import com.github.benmanes.caffeine.cache.stats.StatsCounter;

import io.quarkus.cache.CacheException;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.cache.runtime.AbstractCache;
import io.quarkus.cache.runtime.NullValueConverter;
import io.smallrye.mutiny.Uni;

/**
 * This class is an internal Quarkus cache implementation. Do not use it explicitly from your Quarkus application. The public
 * methods signatures may change without prior notice.
 */
public class CaffeineCacheImpl extends AbstractCache implements CaffeineCache {

    private static final Logger LOGGER = Logger.getLogger(CaffeineCacheImpl.class);

    final AsyncCache<Object, Object> cache;

    private final CaffeineCacheInfo cacheInfo;
    private final StatsCounter statsCounter;

    public CaffeineCacheImpl(CaffeineCacheInfo cacheInfo, boolean recordStats) {
        this.cacheInfo = cacheInfo;
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        if (cacheInfo.initialCapacity != null) {
            builder.initialCapacity(cacheInfo.initialCapacity);
        }
        if (cacheInfo.maximumSize != null) {
            builder.maximumSize(cacheInfo.maximumSize);
        }
        if (cacheInfo.expireAfterWrite != null) {
            builder.expireAfterWrite(cacheInfo.expireAfterWrite);
        }
        if (cacheInfo.expireAfterAccess != null) {
            builder.expireAfterAccess(cacheInfo.expireAfterAccess);
        }
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
        Objects.requireNonNull(key, NULL_KEYS_NOT_SUPPORTED_MSG);
        return Uni.createFrom().completionStage(
                /*
                 * Even if CompletionStage is eager, the Supplier used below guarantees that the cache value computation will be
                 * delayed until subscription time. In other words, the cache value computation is done lazily.
                 */
                new Supplier<CompletionStage<V>>() {
                    @Override
                    public CompletionStage<V> get() {
                        CompletionStage<Object> caffeineValue = getFromCaffeine(key, valueLoader);
                        return cast(caffeineValue);
                    }
                });
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
    private <K, V> CompletableFuture<Object> getFromCaffeine(K key, Function<K, V> valueLoader) {
        CompletableFuture<Object> newCacheValue = new CompletableFuture<>();
        CompletableFuture<Object> existingCacheValue = cache.asMap().putIfAbsent(key, newCacheValue);
        if (existingCacheValue == null) {
            statsCounter.recordMisses(1);
            try {
                Object value = valueLoader.apply(key);
                newCacheValue.complete(NullValueConverter.toCacheValue(value));
            } catch (Throwable t) {
                cache.asMap().remove(key, newCacheValue);
                newCacheValue.complete(new CaffeineComputationThrowable(t));
            }
            return unwrapCacheValueOrThrowable(newCacheValue);
        } else {
            LOGGER.tracef("Key [%s] found in cache [%s]", key, cacheInfo.name);
            statsCounter.recordHits(1);
            return unwrapCacheValueOrThrowable(existingCacheValue);
        }
    }

    private CompletableFuture<Object> unwrapCacheValueOrThrowable(CompletableFuture<Object> cacheValue) {
        return cacheValue.thenApply(new Function<>() {
            @Override
            public Object apply(Object value) {
                // If there's a throwable encapsulated into a CaffeineComputationThrowable, it must be rethrown.
                if (value instanceof CaffeineComputationThrowable) {
                    Throwable cause = ((CaffeineComputationThrowable) value).getCause();
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    } else {
                        throw new CacheException(cause);
                    }
                } else {
                    return NullValueConverter.fromCacheValue(value);
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
    public Uni<Void> replaceUniValue(Object key, Object emittedValue) {
        return Uni.createFrom().item(new Supplier<Void>() {
            @Override
            public Void get() {
                // If the cache no longer contains the key because it was removed, we don't want to put it back.
                cache.asMap().computeIfPresent(key,
                        new BiFunction<Object, CompletableFuture<Object>, CompletableFuture<Object>>() {
                            @Override
                            public CompletableFuture<Object> apply(Object k, CompletableFuture<Object> currentValue) {
                                LOGGER.debugf("Replacing Uni value entry with key [%s] into cache [%s]", key, cacheInfo.name);
                                /*
                                 * The following computed value will always replace the current cache value (whether it is an
                                 * UnresolvedUniValue or not) if this method is called multiple times with the same key.
                                 */
                                return CompletableFuture.completedFuture(NullValueConverter.toCacheValue(emittedValue));
                            }
                        });
                return null;
            }
        });
    }

    @Override
    public Set<Object> keySet() {
        return Collections.unmodifiableSet(new HashSet<>(cache.asMap().keySet()));
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
}
