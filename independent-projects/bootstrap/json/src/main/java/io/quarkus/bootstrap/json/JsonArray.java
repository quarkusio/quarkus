package io.quarkus.bootstrap.json;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public final class JsonArray implements JsonMultiValue {
    private final List<JsonValue> value;

    public JsonArray(List<JsonValue> value) {
        this.value = value;
    }

    public List<JsonValue> value() {
        return value;
    }

    @SuppressWarnings("unchecked")
    public <T extends JsonValue> Stream<T> stream() {
        return (Stream<T>) value.stream();
    }

    /**
     * Applies the given mapping function to each element and returns the results as a list.
     *
     * @param mapper function to apply to each element
     * @param <T> the type of the mapped result
     * @return a list of mapped results
     */
    public <T> List<T> map(Function<JsonValue, T> mapper) {
        List<T> result = new ArrayList<>(value.size());
        for (JsonValue v : value) {
            result.add(mapper.apply(v));
        }
        return result;
    }

    /**
     * Converts each element to a string using {@link Object#toString()} and returns the results as a list.
     *
     * @return a list of string representations of the elements
     */
    public List<String> toStringList() {
        List<String> result = new ArrayList<>(value.size());
        for (JsonValue v : value) {
            result.add(v.toString());
        }
        return result;
    }

    /**
     * Recursively converts this JSON array to a {@link List} of Java objects
     * by calling {@link JsonValue#unwrap()} on each element.
     *
     * @return a mutable list of unwrapped Java values
     */
    public List<Object> toList() {
        List<Object> result = new ArrayList<>(value.size());
        for (JsonValue v : value) {
            result.add(v.unwrap());
        }
        return result;
    }

    @Override
    public void forEach(JsonTransform transform) {
        value.forEach(v -> transform.accept(null, v));
    }

    public int size() {
        return value.size();
    }
}
