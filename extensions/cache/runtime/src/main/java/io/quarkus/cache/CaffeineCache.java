package io.quarkus.cache;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface CaffeineCache extends Cache {

    /**
     * Returns an unmodifiable {@link Set} view of the keys contained in this cache. If the cache entries are modified while an
     * iteration over the set is in progress, the set will remain unchanged.
     *
     * @return a set view of the keys contained in this cache
     */
    Set<Object> keySet();

    /**
     * Returns the future associated with {@code key} in this cache, or {@code null} if there is no
     * cached future for {@code key}. This method is delegating to the
     * {@link com.github.benmanes.caffeine.cache.AsyncCache#getIfPresent(Object)}, while recording the cache stats if they are
     * enabled.
     *
     * @param key key whose associated value is to be returned
     * @return the future value to which the specified key is mapped, or {@code null} if this cache
     *         does not contain a mapping for the key
     * @throws NullPointerException if the specified key is null
     * @see com.github.benmanes.caffeine.cache.AsyncCache#getIfPresent(Object)
     */
    <V> CompletableFuture<V> getIfPresent(Object key);

    /**
     * Associates {@code value} with {@code key} in this cache. If the cache previously contained a
     * value associated with {@code key}, the old value is replaced by {@code value}. If the
     * asynchronous computation fails, the entry will be automatically removed.
     * <p>
     * Prefer {@link #get(Object, Function)} when using the conventional "if cached, return; otherwise
     * create, cache and return" pattern.
     *
     * @param key key with which the specified value is to be associated
     * @param valueFuture value to be associated with the specified key
     * @throws NullPointerException if the specified key or value is null
     */
    <V> void put(Object key, CompletableFuture<V> valueFuture);
}
