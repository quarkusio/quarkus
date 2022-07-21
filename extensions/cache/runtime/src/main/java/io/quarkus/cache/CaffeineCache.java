package io.quarkus.cache;

import java.util.Set;

import io.smallrye.mutiny.Uni;

public interface CaffeineCache extends Cache {

    /**
     * Returns an unmodifiable {@link Set} view of the keys contained in this cache. If the cache entries are modified while an
     * iteration over the set is in progress, the set will remain unchanged.
     *
     * @return a set view of the keys contained in this cache
     */
    Set<Object> keySet();

    /**
     * Returns an {@link Uni} emitting the value for the given key, if the key is contained in this cache. If the key does not
     * exist in the cache, returned {@link Uni} will emit <code>null</code>.
     *
     * @param key key whose associated value is to be returned
     *
     * @return Uni emitting the value to which the specified key is mapped, or emitting <code>null</code> if this cache contains
     *         no mapping for the key
     * @throws NullPointerException if the specified key is null
     */
    <K, V> Uni<V> getIfPresent(K key);
}
