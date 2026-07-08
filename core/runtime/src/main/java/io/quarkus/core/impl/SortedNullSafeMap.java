package io.quarkus.core.impl;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility for building sorted, unmodifiable maps from key-value pairs where
 * null values are silently excluded. Used by the service action codegen to
 * construct the result of {@code consumeAll()} dependencies.
 * <p>
 * Keys must be provided in sorted order. Null values are skipped
 * (the corresponding key is not included in the result).
 */
public final class SortedNullSafeMap {

    private SortedNullSafeMap() {
    }

    /**
     * Return an empty sorted map.
     *
     * @return an empty unmodifiable sorted map
     * @param <V> the value type
     */
    public static <V> Map<String, V> of() {
        return Map.of();
    }

    /**
     * Return a sorted map with up to one entry.
     *
     * @param k1 the key (must not be {@code null})
     * @param v1 the value (may be {@code null}, in which case it is excluded)
     * @return an unmodifiable sorted map
     * @param <V> the value type
     */
    public static <V> Map<String, V> of(String k1, V v1) {
        if (v1 == null) {
            return Map.of();
        }
        return Map.of(k1, v1);
    }

    /**
     * Return a sorted map with up to two entries.
     *
     * @param k1 the first key (must not be {@code null})
     * @param v1 the first value (may be {@code null})
     * @param k2 the second key (must not be {@code null})
     * @param v2 the second value (may be {@code null})
     * @return an unmodifiable sorted map
     * @param <V> the value type
     */
    //@formatter:off
    public static <V> Map<String, V> of(String k1, V v1, String k2, V v2) {
        return v1 == null ? of(k2, v2) :
               v2 == null ? Map.of(k1, v1) :
               ofEntries(k1, k2, v1, v2);
    }
    //@formatter:on

    /**
     * Return a sorted map with up to three entries.
     *
     * @param k1 the first key (must not be {@code null})
     * @param v1 the first value (may be {@code null})
     * @param k2 the second key (must not be {@code null})
     * @param v2 the second value (may be {@code null})
     * @param k3 the third key (must not be {@code null})
     * @param v3 the third value (may be {@code null})
     * @return an unmodifiable sorted map
     * @param <V> the value type
     */
    //@formatter:off
    public static <V> Map<String, V> of(String k1, V v1, String k2, V v2, String k3, V v3) {
        return v1 == null ? of(k2, v2, k3, v3) :
               v2 == null ? of(k1, v1, k3, v3) :
               ofEntries(k1, v1, k2, v2, k3, v3);
    }
    //@formatter:on

    /**
     * Return a sorted map with up to four entries.
     *
     * @param k1 the first key (must not be {@code null})
     * @param v1 the first value (may be {@code null})
     * @param k2 the second key (must not be {@code null})
     * @param v2 the second value (may be {@code null})
     * @param k3 the third key (must not be {@code null})
     * @param v3 the third value (may be {@code null})
     * @param k4 the fourth key (must not be {@code null})
     * @param v4 the fourth value (may be {@code null})
     * @return an unmodifiable sorted map
     * @param <V> the value type
     */
    //@formatter:off
    public static <V> Map<String, V> of(String k1, V v1, String k2, V v2, String k3, V v3, String k4, V v4) {
        return v1 == null ? of(k2, v2, k3, v3, k4, v4) :
               v2 == null ? of(k1, v1, k3, v3, k4, v4) :
               ofEntries(k1, v1, k2, v2, k3, v3, k4, v4);
    }
    //@formatter:on

    /**
     * Return a sorted map from an array of alternating key-value pairs.
     * Keys must be {@code String} and must not be {@code null}.
     * Values may be {@code null}, in which case the entry is excluded.
     *
     * @param entries alternating key-value pairs ({@code key1, value1, key2, value2, ...})
     * @return an unmodifiable sorted map
     * @param <V> the value type
     */
    @SuppressWarnings("unchecked")
    public static <V> Map<String, V> ofEntries(Object... entries) {
        if (entries.length == 0) {
            return Collections.emptySortedMap();
        }
        if ((entries.length & 1) != 0) {
            throw new IllegalArgumentException("Odd number of arguments");
        }
        TreeMap<String, V> map = new TreeMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            String key = (String) entries[i];
            V value = (V) entries[i + 1];
            if (value != null) {
                map.put(key, value);
            }
        }
        if (map.isEmpty()) {
            return Collections.emptySortedMap();
        }
        return Collections.unmodifiableSortedMap(map);
    }
}
