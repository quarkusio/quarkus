package io.quarkus.cache.redis.runtime;

import java.util.function.Function;
import java.util.function.Supplier;

import io.quarkus.cache.Cache;
import io.smallrye.mutiny.Uni;

public interface RedisCache extends Cache {

    /**
     * When configured, gets the default type of the value stored in the cache.
     * The configured type is used when no type is passed into the {@link #get(Object, Class, Function)}.
     *
     * @return the type, {@code null} if not configured.
     */
    Class<?> getDefaultValueType();

    @Override
    default <K, V> Uni<V> get(K key, Function<K, V> valueLoader) {
        Class<V> type = (Class<V>) getDefaultValueType();
        if (type == null) {
            throw new UnsupportedOperationException("Cannot use `get` method without a default type configured. " +
                    "Consider using the `get` method accepting the type or configure the default type for the cache " +
                    getName());
        }
        return get(key, type, valueLoader);
    }

    @SuppressWarnings("unchecked")
    @Override
    default <K, V> Uni<V> getAsync(K key, Function<K, Uni<V>> valueLoader) {
        Class<V> type = (Class<V>) getDefaultValueType();
        if (type == null) {
            throw new UnsupportedOperationException("Cannot use `getAsync` method without a default type configured. " +
                    "Consider using the `getAsync` method accepting the type or configure the default type for the cache " +
                    getName());
        }
        return getAsync(key, type, valueLoader);
    }

    /**
     * Allows retrieving a value from the Redis cache.
     *
     * @param key the key
     * @param clazz the class of the value
     * @param valueLoader the value loader called when there is no value stored in the cache
     * @param <K> the type of key
     * @param <V> the type of value
     * @return the Uni emitting the cached value.
     */
    <K, V> Uni<V> get(K key, Class<V> clazz, Function<K, V> valueLoader);

    /**
     * Allows retrieving a value from the Redis cache.
     *
     * @param key the key
     * @param clazz the class of the value
     * @param valueLoader the value loader called when there is no value stored in the cache
     * @param <K> the type of key
     * @param <V> the type of value
     * @return the Uni emitting the cached value.
     */
    <K, V> Uni<V> getAsync(K key, Class<V> clazz, Function<K, Uni<V>> valueLoader);

    /**
     * Put a value in the cache.
     *
     * @param key the key
     * @param value the value
     * @param <K> the type of key
     * @param <V> the type of value
     * @return a Uni emitting {@code null} when the operation completes
     */
    default <K, V> Uni<Void> put(K key, V value) {
        return put(key, new Supplier<V>() {
            @Override
            public V get() {
                return value;
            }
        });
    }

    <K, V> Uni<Void> put(K key, Supplier<V> supplier);

    <K, V> Uni<V> getOrDefault(K key, V defaultValue);

    <K, V> Uni<V> getOrNull(K key, Class<V> clazz);
}
