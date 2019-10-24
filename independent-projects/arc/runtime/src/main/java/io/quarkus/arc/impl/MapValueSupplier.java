package io.quarkus.arc.impl;

import java.util.Map;
import java.util.function.Supplier;

/**
 * {@link Supplier} implementation that supplies a value from a map. The key is pre-configured.
 *
 * @author Maarten Mulders
 */
public class MapValueSupplier<T> implements Supplier<T> {

    private final Map<String, T> map;
    private final String key;

    public MapValueSupplier(Map<String, T> map, String key) {
        this.map = map;
        this.key = key;
    }

    @Override
    public T get() {
        return map.get(key);
    }
}
