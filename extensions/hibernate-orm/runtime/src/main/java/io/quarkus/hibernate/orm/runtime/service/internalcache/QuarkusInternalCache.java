package io.quarkus.hibernate.orm.runtime.service.internalcache;

import java.util.function.Function;

import org.hibernate.internal.util.cache.InternalCache;

import com.github.benmanes.caffeine.cache.Cache;

final class QuarkusInternalCache<K, V> implements InternalCache<K, V> {

    private final Cache<K, V> cache;

    public QuarkusInternalCache(Cache<K, V> caffeineCache) {
        this.cache = caffeineCache;
    }

    @Override
    public int heldElementsEstimate() {
        return Math.toIntExact(cache.estimatedSize());
    }

    @Override
    public V get(K key) {
        return cache.getIfPresent(key);
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public void clear() {
        cache.invalidateAll();
        cache.cleanUp();
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return cache.get(key, mappingFunction);
    }

}
