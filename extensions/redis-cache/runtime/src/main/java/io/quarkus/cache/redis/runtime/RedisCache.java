package io.quarkus.cache.redis.runtime;

import java.util.Collection;
import java.util.Map;
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

    /**
     * Retrieves all values from the cache for the given collection of keys using a single Redis {@code MGET} command.
     * Only entries that are currently present in the cache are included in the returned map.
     * Keys with no cached value are absent from the result — this is not an error.
     * <p>
     * Requires the default value type to be configured for this cache. Use
     * {@link #getAll(Collection, Class)} to specify the type explicitly.
     *
     * @param keys the collection of keys to look up; must not be {@code null}
     * @param <K> the type of key
     * @param <V> the type of value
     * @return a {@link Uni} emitting a map of cached key-value pairs; missing keys are absent from the map
     * @throws UnsupportedOperationException if no default value type is configured for this cache
     */
    <K, V> Uni<Map<K, V>> getAll(Collection<K> keys);

    /**
     * Retrieves all values from the cache for the given collection of keys using a single Redis {@code MGET} command.
     * Only entries that are currently present in the cache are included in the returned map.
     * Keys with no cached value are absent from the result — this is not an error.
     *
     * @param keys the collection of keys to look up; must not be {@code null}
     * @param clazz the class of the value
     * @param <K> the type of key
     * @param <V> the type of value
     * @return a {@link Uni} emitting a map of cached key-value pairs; missing keys are absent from the map
     */
    <K, V> Uni<Map<K, V>> getAll(Collection<K> keys, Class<V> clazz);

    /**
     * Retrieves all values from the cache for the given collection of keys using a single Redis {@code MGET} command.
     * Only entries that are currently present in the cache are included in the returned map.
     * Keys with no cached value are absent from the result — this is not an error.
     *
     * @param keys the collection of keys to look up; must not be {@code null}
     * @param type the type of the value
     * @param <K> the type of key
     * @param <V> the type of value
     * @return a {@link Uni} emitting a map of cached key-value pairs; missing keys are absent from the map
     */
    <K, V> Uni<Map<K, V>> getAll(Collection<K> keys, TypeLiteral<V> type);

    /**
     * Retrieves all values from the cache for the given collection of keys using a single Redis {@code MGET} command.
     * For any keys not found in the cache, the {@code valueLoader} is invoked once with the complete set of missing keys.
     * The loaded values are written back to the cache automatically.
     * <p>
     * Requires the default value type to be configured for this cache. Use
     * {@link #getAll(Collection, Class, Function)} to specify the type explicitly.
     * <p>
     * Not supported when optimistic locking is enabled for this cache.
     *
     * @param keys the collection of keys to look up; must not be {@code null}
     * @param valueLoader the function called once with all missing keys to compute their values;
     *        must not return {@code null} values
     * @param <K> the type of key
     * @param <V> the type of value
     * @return a {@link Uni} emitting a map containing both cached and newly loaded entries
     * @throws UnsupportedOperationException if no default value type is configured, or if optimistic locking is enabled
     * @throws IllegalArgumentException if the value loader returns a {@code null} value for any key
     */
    <K, V> Uni<Map<K, V>> getAll(Collection<K> keys, Function<Collection<K>, Map<K, V>> valueLoader);

    /**
     * Retrieves all values from the cache for the given collection of keys using a single Redis {@code MGET} command.
     * For any keys not found in the cache, the {@code valueLoader} is invoked once with the complete set of missing keys.
     * The loaded values are written back to the cache automatically.
     * <p>
     * Not supported when optimistic locking is enabled for this cache.
     *
     * @param keys the collection of keys to look up; must not be {@code null}
     * @param clazz the class of the value
     * @param valueLoader the function called once with all missing keys to compute their values;
     *        must not return {@code null} values
     * @param <K> the type of key
     * @param <V> the type of value
     * @return a {@link Uni} emitting a map containing both cached and newly loaded entries
     * @throws UnsupportedOperationException if optimistic locking is enabled for this cache
     * @throws IllegalArgumentException if the value loader returns a {@code null} value for any key
     */
    <K, V> Uni<Map<K, V>> getAll(Collection<K> keys, Class<V> clazz, Function<Collection<K>, Map<K, V>> valueLoader);

    /**
     * Retrieves all values from the cache for the given collection of keys using a single Redis {@code MGET} command.
     * For any keys not found in the cache, the {@code valueLoader} is invoked once with the complete set of missing keys.
     * The loaded values are written back to the cache automatically.
     * <p>
     * Not supported when optimistic locking is enabled for this cache.
     *
     * @param keys the collection of keys to look up; must not be {@code null}
     * @param type the type of the value
     * @param valueLoader the function called once with all missing keys to compute their values;
     *        must not return {@code null} values
     * @param <K> the type of key
     * @param <V> the type of value
     * @return a {@link Uni} emitting a map containing both cached and newly loaded entries
     * @throws UnsupportedOperationException if optimistic locking is enabled for this cache
     * @throws IllegalArgumentException if the value loader returns a {@code null} value for any key
     */
    <K, V> Uni<Map<K, V>> getAll(Collection<K> keys, TypeLiteral<V> type, Function<Collection<K>, Map<K, V>> valueLoader);

    /**
     * Retrieves all values from the cache for the given collection of keys using a single Redis {@code MGET} command.
     * For any keys not found in the cache, the async {@code valueLoader} is invoked once with the complete set of
     * missing keys. The loaded values are written back to the cache automatically.
     * <p>
     * Requires the default value type to be configured for this cache. Use
     * {@link #getAllAsync(Collection, Class, Function)} to specify the type explicitly.
     * <p>
     * Not supported when optimistic locking is enabled for this cache.
     *
     * @param keys the collection of keys to look up; must not be {@code null}
     * @param valueLoader the async function called once with all missing keys to compute their values;
     *        the emitted map must not contain {@code null} values
     * @param <K> the type of key
     * @param <V> the type of value
     * @return a {@link Uni} emitting a map containing both cached and newly loaded entries
     * @throws UnsupportedOperationException if no default value type is configured, or if optimistic locking is enabled
     * @throws IllegalArgumentException if the value loader emits a {@code null} value for any key
     */
    <K, V> Uni<Map<K, V>> getAllAsync(Collection<K> keys, Function<Collection<K>, Uni<Map<K, V>>> valueLoader);

    /**
     * Retrieves all values from the cache for the given collection of keys using a single Redis {@code MGET} command.
     * For any keys not found in the cache, the async {@code valueLoader} is invoked once with the complete set of
     * missing keys. The loaded values are written back to the cache automatically.
     * <p>
     * Not supported when optimistic locking is enabled for this cache.
     *
     * @param keys the collection of keys to look up; must not be {@code null}
     * @param clazz the class of the value
     * @param valueLoader the async function called once with all missing keys to compute their values;
     *        the emitted map must not contain {@code null} values
     * @param <K> the type of key
     * @param <V> the type of value
     * @return a {@link Uni} emitting a map containing both cached and newly loaded entries
     * @throws UnsupportedOperationException if optimistic locking is enabled for this cache
     * @throws IllegalArgumentException if the value loader emits a {@code null} value for any key
     */
    <K, V> Uni<Map<K, V>> getAllAsync(Collection<K> keys, Class<V> clazz,
            Function<Collection<K>, Uni<Map<K, V>>> valueLoader);

    /**
     * Retrieves all values from the cache for the given collection of keys using a single Redis {@code MGET} command.
     * For any keys not found in the cache, the async {@code valueLoader} is invoked once with the complete set of
     * missing keys. The loaded values are written back to the cache automatically.
     * <p>
     * Not supported when optimistic locking is enabled for this cache.
     *
     * @param keys the collection of keys to look up; must not be {@code null}
     * @param type the type of the value
     * @param valueLoader the async function called once with all missing keys to compute their values;
     *        the emitted map must not contain {@code null} values
     * @param <K> the type of key
     * @param <V> the type of value
     * @return a {@link Uni} emitting a map containing both cached and newly loaded entries
     * @throws UnsupportedOperationException if optimistic locking is enabled for this cache
     * @throws IllegalArgumentException if the value loader emits a {@code null} value for any key
     */
    <K, V> Uni<Map<K, V>> getAllAsync(Collection<K> keys, TypeLiteral<V> type,
            Function<Collection<K>, Uni<Map<K, V>>> valueLoader);

    /**
     * Stores all entries from the given map in the cache using a single Redis operation.
     * <p>
     * When no expiration is configured, a single atomic {@code MSET} command is used.
     * When {@code expire-after-write} is configured, individual {@code SET} commands with a TTL are
     * pipelined in a single round-trip, since {@code MSET} does not support per-key expiry.
     *
     * @param map the entries to store; must not be {@code null} and must not contain {@code null} values
     * @param <K> the type of key
     * @param <V> the type of value
     * @return a {@link Uni} emitting {@code null} when the operation completes
     * @throws IllegalArgumentException if the map contains a {@code null} value
     */
    <K, V> Uni<Void> putAll(Map<K, V> map);
}
