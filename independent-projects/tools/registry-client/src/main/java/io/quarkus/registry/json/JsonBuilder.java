package io.quarkus.registry.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import io.quarkus.registry.config.RegistryConfigImpl;

/**
 * Serialization detail. Not part of the Catalog or Config API.
 */
public interface JsonBuilder<T> {
    T build();

    /**
     * Make sure a JsonBuilder is built before being serialized
     */
    public class JsonBuilderSerializer<T> extends JsonSerializer<JsonBuilder<T>> {
        @Override
        public void serialize(JsonBuilder<T> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeObject(value.build());
        }
    }

    static <T> List<T> modifiableListOrNull(Collection<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            if (list instanceof List) {
                list.addAll(Collections.emptyList()); // test for immutable list
                return (List<T>) list;
            }
        } catch (UnsupportedOperationException immutableMap) {
        }
        return new ArrayList<>(list);
    }

    static <K, V> Map<K, V> modifiableMapOrNull(Map<K, V> map, Supplier<Map<K, V>> mapSupplier) {
        // when building from an existing immutable impl, the empty map will be returned
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            map.putAll(Collections.emptyMap()); // test for immutable map
            return map;
        } catch (UnsupportedOperationException immutableMap) {
            Map<K, V> result = mapSupplier.get();
            result.putAll(map);
            return result;
        }
    }

    static <T> List<T> toUnmodifiableList(Collection<T> o) {
        if (o == null || o.isEmpty()) {
            return Collections.emptyList();
        }
        return List.copyOf(o);
    }

    static <K, V> Map<K, V> toUnmodifiableMap(Map<K, V> map) {
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }
        return Map.copyOf(map);
    }

    static <T> T buildIfBuilder(T o) {
        if (o instanceof JsonBuilder<?>) {
            return (T) ((JsonBuilder<?>) o).build();
        }
        return o;
    }

    static <S> S buildIfBuilder(S o, Class<S> clazz) {
        if (o instanceof JsonBuilder<?>) {
            return (S) ((JsonBuilder<?>) o).build();
        }
        return o;
    }

    static <T> List<T> buildersToUnmodifiableList(List<T> o) {
        if (o == null || o.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>(o.size());
        o.forEach(x -> result.add(buildIfBuilder(x)));
        return Collections.unmodifiableList(result);
    }

    static <K, V> Map<K, V> buildUnmodifiableMap(Map<K, V> map, Supplier<Map<K, V>> supplier) {
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<K, V> result = supplier.get();
        map.forEach((k, v) -> result.put(k, buildIfBuilder(v)));
        return Collections.unmodifiableMap(result);
    }

    static void ensureNextToken(JsonParser p, JsonToken expected, DeserializationContext ctxt) throws IOException {
        if (p.nextToken() != expected) {
            throw InvalidFormatException.from(p, "Expected " + expected, ctxt, RegistryConfigImpl.Builder.class);
        }
    }
}
