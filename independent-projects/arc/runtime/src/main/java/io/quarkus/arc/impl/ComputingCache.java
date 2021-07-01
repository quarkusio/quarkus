package io.quarkus.arc.impl;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Computing cache backed by a {@link ConcurrentHashMap} which intentionally does not use
 * {@link Map#computeIfAbsent(Object, Function)} and is reentrant.
 * Derived from {@code org.jboss.weld.util.cache.ReentrantMapBackedComputingCache}.
 *
 * @param <K>
 * @param <V>
 */
public class ComputingCache<K, V> {

    private final ConcurrentMap<K, LazyValue<V>> map;
    private final Function<K, V> computingFunction;

    /**
     * Note that {@link #getValue(Object)} cannot be used if no default computing function is specified.
     */
    public ComputingCache() {
        this(null);
    }

    public ComputingCache(Function<K, V> computingFunction) {
        this.map = new ConcurrentHashMap<>();
        this.computingFunction = computingFunction;
    }

    public V getValue(K key) {
        return computeIfAbsent(key, computingFunction);
    }

    public V getValueIfPresent(K key) {
        LazyValue<V> value = map.get(key);
        return value != null ? value.getIfPresent() : null;
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> computingFunction) {
        return computeIfAbsent(key, new Supplier<V>() {
            @Override
            public V get() {
                return computingFunction.apply(key);
            }
        });
    }

    public V computeIfAbsent(K key, Supplier<V> supplier) {
        if (supplier == null) {
            throw new IllegalStateException("Computing function not defined");
        }
        LazyValue<V> value = map.get(key);
        if (value == null) {
            value = new LazyValue<V>(supplier);
            LazyValue<V> previous = map.putIfAbsent(key, value);
            if (previous != null) {
                value = previous;
            }
        }
        return value.get();
    }

    public V remove(K key) {
        LazyValue<V> previous = map.remove(key);
        return previous != null ? previous.get() : null;
    }

    public void clear() {
        map.clear();
    }

    public void forEachValue(Consumer<? super V> action) {
        Objects.requireNonNull(action);
        for (LazyValue<V> value : map.values()) {
            action.accept(value.get());
        }
    }

    public void forEachExistingValue(Consumer<? super V> action) {
        Objects.requireNonNull(action);
        for (LazyValue<V> value : map.values()) {
            if (value.isSet()) {
                action.accept(value.get());
            }
        }
    }

    public Set<V> getPresentValues() {
        return map.values().stream().map(LazyValue::getIfPresent).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public void forEachEntry(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        map.forEach((k, v) -> action.accept(k, v.get()));
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

}
