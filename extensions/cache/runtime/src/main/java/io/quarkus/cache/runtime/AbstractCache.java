package io.quarkus.cache.runtime;

import io.quarkus.cache.Cache;
import io.quarkus.cache.DefaultCacheKey;
import io.smallrye.mutiny.Uni;

public abstract class AbstractCache implements Cache {

    public static final String NULL_KEYS_NOT_SUPPORTED_MSG = "Null keys are not supported by the Quarkus application data cache";

    private Object defaultKey;

    @Override
    public Object getDefaultKey() {
        if (defaultKey == null) {
            defaultKey = new DefaultCacheKey(getName());
        }
        return defaultKey;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Cache> T as(Class<T> type) {
        if (type.isInstance(this)) {
            return (T) this;
        } else {
            throw new IllegalStateException("This cache is not an instance of " + type.getName());
        }
    }

    /**
     * Replaces the cache value associated with the given key by an item emitted by a {@link Uni}. This method can be called
     * several times for the same key, each call will then always replace the existing cache entry with the given emitted
     * value. If the key no longer identifies a cache entry, this method must not put the emitted item into the cache.
     */
    public abstract Uni<Void> replaceUniValue(Object key, Object emittedValue);
}
