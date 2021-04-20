package io.quarkus.cache.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.quarkus.cache.Cache;
import io.smallrye.mutiny.Uni;

public abstract class AbstractCache implements Cache {

    public static final String NULL_KEYS_NOT_SUPPORTED_MSG = "Null keys are not supported by the Quarkus application data cache";

    private Object defaultKey;

    public abstract String getName();

    /**
     * Returns the unique and immutable default key for the current cache. This key is used by the annotations caching API when
     * a no-args method annotated with {@link io.quarkus.cache.CacheResult CacheResult} or
     * {@link io.quarkus.cache.CacheInvalidate CacheInvalidate} is invoked.
     * 
     * @return default cache key
     */
    public Object getDefaultKey() {
        if (defaultKey == null) {
            defaultKey = new DefaultCacheKey(getName());
        }
        return defaultKey;
    }

    public abstract CompletableFuture<Object> get(Object key, Function<Object, Object> valueLoader);

    public abstract void invalidate(Object key);

    public abstract void invalidateAll();

    /**
     * Replaces the cache value associated with the given key by an item emitted by a {@link Uni}. This method can be called
     * several times for the same key, each call will then always replace the existing cache entry with the given emitted
     * value. If the key no longer identifies a cache entry, this method must not put the emitted item into the cache.
     */
    public abstract Uni<Void> replaceUniValue(Object key, Object emittedValue);
}
