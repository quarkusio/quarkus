package io.quarkus.deployment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SupplierMap<K, V> implements Map<K, V> {
    private final Map<K, Supplier<V>> suppliers;
    private final Map<K, V> cache;

    public SupplierMap() {
        this.suppliers = new HashMap<>();
        this.cache = new HashMap<>();
    }

    public void put(K key, Supplier<V> supplier) {
        suppliers.put(key, supplier);
    }

    @Override
    public V get(Object key) {
        if (key == null) {
            return null;
        }
        return cache.computeIfAbsent((K) key, k -> {
            Supplier<V> supplier = suppliers.get(k);
            if (supplier == null) {
                return null;
            }
            return supplier.get();
        });
    }

    public void clear() {
        suppliers.clear();
    }

    @Override
    public Set<K> keySet() {
        return suppliers.keySet();
    }

    @Override
    public Collection<V> values() {
        return suppliers.values().stream().map(Supplier::get).toList();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return suppliers.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().get()))
                .collect(Collectors.toSet());
    }

    public int size() {
        return suppliers.size();
    }

    @Override
    public boolean isEmpty() {
        return suppliers.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return suppliers.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V put(K key, V value) {
        Supplier<V> old = suppliers.put(key, () -> value);
        if (old == null) {
            return null;
        }
        return old.get();
    }

    @Override
    public V remove(Object key) {
        Supplier<? extends V> remove = suppliers.remove(key);
        if (remove == null) {
            return null;
        }
        return remove.get();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            cache.put(entry.getKey(), entry.getValue());
        }
    }

    public Map<K, V> asEagerMap() {
        Map<K, V> result = new HashMap<>();
        for (K key : suppliers.keySet()) {
            result.put(key, get(key));
        }
        return result;
    }
}
