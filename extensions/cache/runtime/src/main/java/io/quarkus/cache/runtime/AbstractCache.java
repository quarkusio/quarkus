package io.quarkus.cache.runtime;

import io.quarkus.cache.Cache;
import io.quarkus.cache.DefaultCacheKey;

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

}
