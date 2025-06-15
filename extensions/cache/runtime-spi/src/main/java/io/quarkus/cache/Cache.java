package io.quarkus.cache;

import java.util.function.Function;
import java.util.function.Predicate;

import io.smallrye.mutiny.Uni;

/**
 * Use this interface to interact with a cache programmatically e.g. store, retrieve or delete cache values. The cache
 * can be injected using the {@code @io.quarkus.cache.CacheName} annotation or retrieved using
 * {@link CacheManager#getCache(String)}.
 */
public interface Cache {

    /**
     * Returns the cache name.
     *
     * @return cache name
     */
    String getName();

    /**
     * Returns the unique and immutable default key for the current cache. This key is used by the annotations caching
     * API when a no-args method annotated with {@code @io.quarkus.cache.CacheResult} or
     * {@code @io.quarkus.cache.CacheInvalidate} is invoked. It can also be used with the programmatic caching API.
     *
     * @return default cache key
     */
    Object getDefaultKey();

    /**
     * Returns a lazy asynchronous action that will emit the cache value identified by {@code key}, obtaining that value
     * from {@code valueLoader} if necessary.
     *
     * @param <K>
     *        cache key type
     * @param <V>
     *        cache value type
     * @param key
     *        cache key
     * @param valueLoader
     *        function used to compute a cache value if {@code key} is not already associated with a value
     *
     * @return a lazy asynchronous action that will emit a cache value
     *
     * @throws NullPointerException
     *         if the key is {@code null}
     * @throws CacheException
     *         if an exception is thrown during a cache value computation
     */
    <K, V> Uni<V> get(K key, Function<K, V> valueLoader);

    /**
     * Returns a lazy asynchronous action that will emit the cache value identified by {@code key}, obtaining that value
     * from {@code valueLoader} if necessary.
     *
     * @param <K>
     * @param <V>
     * @param key
     * @param valueLoader
     *
     * @return a lazy asynchronous action that will emit a cache value
     *
     * @throws NullPointerException
     *         if the key is {@code null}
     */
    <K, V> Uni<V> getAsync(K key, Function<K, Uni<V>> valueLoader);

    /**
     * Removes the cache entry identified by {@code key} from the cache. If the key does not identify any cache entry,
     * nothing will happen.
     *
     * @param key
     *        cache key
     *
     * @throws NullPointerException
     *         if the key is {@code null}
     */
    Uni<Void> invalidate(Object key);

    /**
     * Removes all entries from the cache.
     */
    Uni<Void> invalidateAll();

    /**
     * Removes all cache entries whose keys match the given predicate.
     *
     * @param predicate
     */
    Uni<Void> invalidateIf(Predicate<Object> predicate);

    /**
     * Returns this cache as an instance of the provided type if possible.
     *
     * @return cache instance of the provided type
     *
     * @throws IllegalStateException
     *         if this cache is not an instance of {@code type}
     */
    <T extends Cache> T as(Class<T> type);

}
