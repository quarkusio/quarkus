package io.quarkus.cache.runtime;

import java.time.Duration;
import java.util.function.Function;

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
     * Synchronous compute-if-absent used by {@code @CacheResult} on non-async methods.
     * <p>
     * Default implementation uses {@link #get(Object, Function)} + {@code Uni.await()}.
     * Caffeine overrides this to join the underlying {@link java.util.concurrent.CompletableFuture}
     * without Mutiny, which avoids SmallRye Context Propagation clearing an activated CDI request
     * context when a {@code ManagedExecutor} has {@code cleared=CDI}.
     */
    public <K, V> V getSynchronous(K key, Function<K, V> valueLoader) {
        return get(key, valueLoader).await().indefinitely();
    }

    /**
     * Same as {@link #getSynchronous(Object, Function)} with a lock timeout in milliseconds.
     * Throws {@link io.smallrye.mutiny.TimeoutException} if the wait times out.
     */
    public <K, V> V getSynchronous(K key, Function<K, V> valueLoader, long lockTimeoutMillis) {
        if (lockTimeoutMillis <= 0) {
            return getSynchronous(key, valueLoader);
        }
        return get(key, valueLoader).await().atMost(Duration.ofMillis(lockTimeoutMillis));
    }

}
