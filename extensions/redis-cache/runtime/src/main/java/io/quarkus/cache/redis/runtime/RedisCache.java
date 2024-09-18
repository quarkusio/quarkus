package io.quarkus.cache.redis.runtime;

import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.util.TypeLiteral;

import io.quarkus.cache.Cache;
import io.smallrye.mutiny.Uni;

public interface RedisCache extends Cache {

    /**
     * When configured, gets the default type of the value stored in the cache.
     * The configured type is used in methods {@link #get(Object, Function)},
     * {@link #getAsync(Object, Function)}, {@link #getOrDefault(Object, Object)}
     * and {@link #getOrNull(Object)}.
     *
     * @deprecated should have never been exposed publicly
     * @return the type, {@code null} if not configured or if not a {@code Class}.
     */
    @Deprecated
    Class<?> getDefaultValueType();

    @Override
    <K, V> Uni<V> get(K key, Function<K, V> valueLoader);

    @Override
    <K, V> Uni<V> getAsync(K key, Function<K, Uni<V>> valueLoader);

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
     * @param type the type of the value
     * @param valueLoader the value loader called when there is no value stored in the cache
     * @param <K> the type of key
     * @param <V> the type of value
     * @return the Uni emitting the cached value.
     */
    <K, V> Uni<V> get(K key, TypeLiteral<V> type, Function<K, V> valueLoader);

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
     * Allows retrieving a value from the Redis cache.
     *
     * @param key the key
     * @param type the type of the value
     * @param valueLoader the value loader called when there is no value stored in the cache
     * @param <K> the type of key
     * @param <V> the type of value
     * @return the Uni emitting the cached value.
     */
    <K, V> Uni<V> getAsync(K key, TypeLiteral<V> type, Function<K, Uni<V>> valueLoader);

    /**
     * Put a value in the cache.
     *
     * @param key the key
     * @param value the value
     * @param <K> the type of key
     * @param <V> the type of value
     * @return a Uni emitting {@code null} when the operation completes
     */
    <K, V> Uni<Void> put(K key, V value);

    /**
     * Put a value in the cache.
     *
     * @param key the key
     * @param supplier supplier of the value
     * @param <K> the type of key
     * @param <V> the type of value
     * @return a Uni emitting {@code null} when the operation completes
     */
    <K, V> Uni<Void> put(K key, Supplier<V> supplier);

    /**
     * Returns {@link Uni} that completes with a value present in the cache under the given {@code key}.
     * If there is no value in the cache under the key, the {@code Uni} completes with the given {@code defaultValue}.
     *
     * @param key the key
     * @param defaultValue the default value
     * @param <K> the type of key
     * @param <V> the type of value
     * @return a Uni emitting the value cached under {@code key}, or {@code defaultValue} if there is no cached value
     */
    <K, V> Uni<V> getOrDefault(K key, V defaultValue);

    /**
     * Returns {@link Uni} that completes with a value present in the cache under the given {@code key}.
     * If there is no value in the cache under the key, the {@code Uni} completes with the given {@code defaultValue}.
     *
     * @param key the key
     * @param clazz class of the value
     * @param defaultValue the default value
     * @param <K> the type of key
     * @param <V> the type of value
     * @return a Uni emitting the value cached under {@code key}, or {@code defaultValue} if there is no cached value
     */
    <K, V> Uni<V> getOrDefault(K key, Class<V> clazz, V defaultValue);

    /**
     * Returns {@link Uni} that completes with a value present in the cache under the given {@code key}.
     * If there is no value in the cache under the key, the {@code Uni} completes with the given {@code defaultValue}.
     *
     * @param key the key
     * @param type type of the value
     * @param defaultValue the default value
     * @param <K> the type of key
     * @param <V> the type of value
     * @return a Uni emitting the value cached under {@code key}, or {@code defaultValue} if there is no cached value
     */
    <K, V> Uni<V> getOrDefault(K key, TypeLiteral<V> type, V defaultValue);

    /**
     * Returns {@link Uni} that completes with a value present in the cache under the given {@code key}.
     * If there is no value in the cache under the key, the {@code Uni} completes with {@code null}.
     *
     * @param key the key
     * @param <K> the type of key
     * @param <V> the type of value
     * @return a Uni emitting the value cached under {@code key}, or {@code null} if there is no cached value
     */
    <K, V> Uni<V> getOrNull(K key);

    /**
     * Returns {@link Uni} that completes with a value present in the cache under the given {@code key}.
     * If there is no value in the cache under the key, the {@code Uni} completes with {@code null}.
     *
     * @param key the key
     * @param clazz the class of the value
     * @param <K> the type of key
     * @param <V> the type of value
     * @return a Uni emitting the value cached under {@code key}, or {@code null} if there is no cached value
     */
    <K, V> Uni<V> getOrNull(K key, Class<V> clazz);

    /**
     * Returns {@link Uni} that completes with a value present in the cache under the given {@code key}.
     * If there is no value in the cache under the key, the {@code Uni} completes with {@code null}.
     *
     * @param key the key
     * @param type the type of the value
     * @param <K> the type of key
     * @param <V> the type of value
     * @return a Uni emitting the value cached under {@code key}, or {@code null} if there is no cached value
     */
    <K, V> Uni<V> getOrNull(K key, TypeLiteral<V> type);
}
