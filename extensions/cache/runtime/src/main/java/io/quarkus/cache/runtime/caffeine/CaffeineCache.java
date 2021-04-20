package io.quarkus.cache.runtime.caffeine;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.jboss.logging.Logger;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.quarkus.cache.runtime.AbstractCache;
import io.quarkus.cache.runtime.CacheException;
import io.quarkus.cache.runtime.NullValueConverter;
import io.smallrye.mutiny.Uni;

/**
 * This class is an internal Quarkus cache implementation. Do not use it explicitly from your Quarkus application. The public
 * methods signatures may change without prior notice.
 */
public class CaffeineCache extends AbstractCache {

    private static final Logger LOGGER = Logger.getLogger(CaffeineCache.class);

    private AsyncCache<Object, Object> cache;

    private String name;

    private Integer initialCapacity;

    private Long maximumSize;

    private Duration expireAfterWrite;

    private Duration expireAfterAccess;

    public CaffeineCache(CaffeineCacheInfo cacheInfo) {
        this.name = cacheInfo.name;
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        if (cacheInfo.initialCapacity != null) {
            this.initialCapacity = cacheInfo.initialCapacity;
            builder.initialCapacity(cacheInfo.initialCapacity);
        }
        if (cacheInfo.maximumSize != null) {
            this.maximumSize = cacheInfo.maximumSize;
            builder.maximumSize(cacheInfo.maximumSize);
        }
        if (cacheInfo.expireAfterWrite != null) {
            this.expireAfterWrite = cacheInfo.expireAfterWrite;
            builder.expireAfterWrite(cacheInfo.expireAfterWrite);
        }
        if (cacheInfo.expireAfterAccess != null) {
            this.expireAfterAccess = cacheInfo.expireAfterAccess;
            builder.expireAfterAccess(cacheInfo.expireAfterAccess);
        }
        cache = builder.buildAsync();
    }

    @Override
    public String getName() {
        return name;
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
    @Override
    public CompletableFuture<Object> get(Object key, Function<Object, Object> valueLoader) {
        if (key == null) {
            throw new NullPointerException(NULL_KEYS_NOT_SUPPORTED_MSG);
        }
        CompletableFuture<Object> newCacheValue = new CompletableFuture<>();
        CompletableFuture<Object> existingCacheValue = cache.asMap().putIfAbsent(key, newCacheValue);
        if (existingCacheValue == null) {
            try {
                Object value = valueLoader.apply(key);
                newCacheValue.complete(NullValueConverter.toCacheValue(value));
            } catch (Throwable t) {
                cache.asMap().remove(key, newCacheValue);
                newCacheValue.complete(new CaffeineComputationThrowable(t));
            }
            return unwrapCacheValueOrThrowable(newCacheValue);
        } else {
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
    public void invalidate(Object key) {
        if (key == null) {
            throw new NullPointerException(NULL_KEYS_NOT_SUPPORTED_MSG);
        }
        cache.synchronous().invalidate(key);
    }

    @Override
    public void invalidateAll() {
        cache.synchronous().invalidateAll();
    }

    @Override
    public Uni<Void> replaceUniValue(Object key, Object emittedValue) {
        return Uni.createFrom().item(() -> {
            // If the cache no longer contains the key because it was removed, we don't want to put it back.
            cache.asMap().computeIfPresent(key, (k, currentValue) -> {
                LOGGER.debugf("Replacing Uni value entry with key [%s] into cache [%s]", key, name);
                /*
                 * The following computed value will always replace the current cache value (whether it is an
                 * UnresolvedUniValue or not) if this method is called multiple times with the same key.
                 */
                return CompletableFuture.completedFuture(NullValueConverter.toCacheValue(emittedValue));
            });
            return null;
        });
    }

    // For testing purposes only.
    public Integer getInitialCapacity() {
        return initialCapacity;
    }

    // For testing purposes only.
    public Long getMaximumSize() {
        return maximumSize;
    }

    // For testing purposes only.
    public Duration getExpireAfterWrite() {
        return expireAfterWrite;
    }

    // For testing purposes only.
    public Duration getExpireAfterAccess() {
        return expireAfterAccess;
    }

    public long getSize() {
        return cache.synchronous().estimatedSize();
    }
}
