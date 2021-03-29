package io.quarkus.devtools.codestarts.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class NestedMaps {

    private NestedMaps() {
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getValue(Map<String, Object> data, String path) {
        if (!path.contains(".")) {
            return Optional.ofNullable((T) data.get(path));
        }
        int index = path.indexOf(".");
        String key = path.substring(0, index);
        if (data.get(key) instanceof Map) {
            return getValue((Map<String, Object>) data.get(key), path.substring(index + 1));
        } else {
            return Optional.empty();
        }

    }

    public static <T> Map<String, T> deepMerge(final Stream<Map<String, T>> mapStream) {
        final Map<String, T> out = new HashMap<>();
        mapStream.forEach(m -> deepMerge(out, m));
        return out;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void deepMerge(Map left, Map right) {
        for (Object key : right.keySet()) {
            Object rightValue = right.get(key);
            Object leftValue = left.get(key);

            if (rightValue instanceof Map && leftValue instanceof Map) {
                deepMerge((Map) leftValue, (Map) rightValue);
            } else if (rightValue instanceof Collection && leftValue instanceof Collection) {
                Collection c = new LinkedHashSet();
                c.addAll((Collection) leftValue);
                c.addAll((Collection) rightValue);
                left.put(key, c);
            } else {
                // Override
                left.put(key, rightValue);
            }
        }
    }

    public static Map<String, Object> unflatten(Map<String, Object> flattened) {
        Map<String, Object> unflattened = new HashMap<>();
        for (Map.Entry<String, Object> entry : flattened.entrySet()) {
            doUnflatten(unflattened, entry.getKey(), entry.getValue());
        }
        return unflattened;
    }

    private static void doUnflatten(Map<String, Object> current, String key, Object originalValue) {
        String[] parts = key.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == (parts.length - 1)) {
                if (current.containsKey(part)) {
                    throw new IllegalStateException("Conflicting data types for key '" + key + "'");
                }
                current.put(part, originalValue);
                return;
            }

            final Object value = current.get(part);
            if (value == null) {
                final HashMap<String, Object> map = new HashMap<>();
                current.put(part, map);
                current = map;
            } else if (value instanceof Map) {
                current = (Map<String, Object>) value;
            } else {
                throw new IllegalStateException("Conflicting data types for key '" + key + "'");
            }
        }
    }

}
