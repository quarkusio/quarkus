package io.quarkus.bootstrap.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class JsonObject implements JsonMultiValue {
    private final Map<String, JsonValue> value;

    public JsonObject(Map<String, JsonValue> value) {
        this.value = value;
    }

    /**
     * Returns the raw {@link JsonValue} for the given attribute, or {@code null} if absent.
     *
     * @param attribute attribute name
     * @return the JSON value, or {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T extends JsonValue> T get(String attribute) {
        return (T) value.get(attribute);
    }

    /**
     * Unwraps the value of the given attribute as a {@link String}.
     *
     * @param attribute attribute name
     * @return the string value, or {@code null} if the attribute is absent
     * @throws IllegalArgumentException if the attribute is present but is not a JSON string
     */
    public String unwrapString(String attribute) {
        JsonValue v = get(attribute);
        if (v == null) {
            return null;
        }
        if (v instanceof JsonString s) {
            return s.value();
        }
        throw typeMismatch(attribute, "string", v);
    }

    /**
     * Unwraps the value of the given attribute as a {@code boolean}.
     *
     * @param attribute attribute name
     * @return the boolean value, or {@code false} if the attribute is absent
     * @throws IllegalArgumentException if the attribute is present but is not a JSON boolean
     */
    public boolean unwrapBoolean(String attribute) {
        JsonValue v = get(attribute);
        if (v == null) {
            return false;
        }
        if (v instanceof JsonBoolean b) {
            return b.value();
        }
        throw typeMismatch(attribute, "boolean", v);
    }

    /**
     * Unwraps the value of the given attribute as an {@code int}.
     *
     * @param attribute attribute name
     * @param defaultValue value to return if the attribute is absent
     * @return the int value, or {@code defaultValue} if the attribute is absent
     * @throws IllegalArgumentException if the attribute is present but is not a JSON integer
     */
    public int unwrapInt(String attribute, int defaultValue) {
        JsonValue v = get(attribute);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof JsonInteger n) {
            return n.intValue();
        }
        throw typeMismatch(attribute, "integer", v);
    }

    /**
     * Unwraps the value of the given attribute as a {@code long}.
     *
     * @param attribute attribute name
     * @param defaultValue value to return if the attribute is absent
     * @return the long value, or {@code defaultValue} if the attribute is absent
     * @throws IllegalArgumentException if the attribute is present but is not a JSON integer
     */
    public long unwrapLong(String attribute, long defaultValue) {
        JsonValue v = get(attribute);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof JsonInteger n) {
            return n.longValue();
        }
        throw typeMismatch(attribute, "integer", v);
    }

    /**
     * Unwraps the value of the given attribute as a {@code double}.
     * JSON integers are accepted and widened to double.
     *
     * @param attribute attribute name
     * @param defaultValue value to return if the attribute is absent
     * @return the double value, or {@code defaultValue} if the attribute is absent
     * @throws IllegalArgumentException if the attribute is present but is not a JSON number
     */
    public double unwrapDouble(String attribute, double defaultValue) {
        JsonValue v = get(attribute);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof JsonDouble d) {
            return d.value();
        }
        if (v instanceof JsonInteger n) {
            return n.longValue();
        }
        throw typeMismatch(attribute, "number", v);
    }

    /**
     * Returns the value of the given attribute as a {@link JsonObject}.
     *
     * @param attribute attribute name
     * @return the JSON object, or {@code null} if the attribute is absent
     * @throws IllegalArgumentException if the attribute is present but is not a JSON object
     */
    public JsonObject unwrapObject(String attribute) {
        JsonValue v = get(attribute);
        if (v == null) {
            return null;
        }
        if (v instanceof JsonObject o) {
            return o;
        }
        throw typeMismatch(attribute, "object", v);
    }

    /**
     * Returns the value of the given attribute as a {@link JsonArray}.
     *
     * @param attribute attribute name
     * @return the JSON array, or {@code null} if the attribute is absent
     * @throws IllegalArgumentException if the attribute is present but is not a JSON array
     */
    public JsonArray unwrapArray(String attribute) {
        JsonValue v = get(attribute);
        if (v == null) {
            return null;
        }
        if (v instanceof JsonArray a) {
            return a;
        }
        throw typeMismatch(attribute, "array", v);
    }

    /**
     * Unwraps the array at the given attribute and maps each element through the provided function.
     * Each array element is expected to be a {@link JsonObject}.
     *
     * @param attribute attribute name
     * @param mapper function to apply to each JSON object element
     * @return the mapped list, or an empty list if the attribute is absent
     * @throws IllegalArgumentException if the attribute is present but is not a JSON array
     */
    public <T> List<T> mapArray(String attribute, Function<JsonObject, T> mapper) {
        JsonArray arr = unwrapArray(attribute);
        if (arr == null) {
            return List.of();
        }
        return arr.map(v -> mapper.apply((JsonObject) v));
    }

    /**
     * Unwraps the array at the given attribute as a list of strings.
     * Each element is converted using {@link Object#toString()}.
     *
     * @param attribute attribute name
     * @return the string list, or an empty list if the attribute is absent
     * @throws IllegalArgumentException if the attribute is present but is not a JSON array
     */
    public List<String> unwrapStringList(String attribute) {
        JsonArray arr = unwrapArray(attribute);
        if (arr == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>(arr.size());
        for (JsonValue v : arr.value()) {
            result.add(v.toString());
        }
        return result;
    }

    /**
     * Recursively converts this JSON object to a {@link Map}.
     * Nested JSON objects become nested maps, JSON arrays become lists,
     * and scalar values are unwrapped to their Java equivalents
     * ({@link String}, {@link Integer}, {@link Long}, {@link Double}, {@link Boolean}, or {@code null}).
     *
     * @return a mutable map representation of this JSON object
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>(value.size());
        for (var entry : value.entrySet()) {
            map.put(entry.getKey(), entry.getValue().unwrap());
        }
        return map;
    }

    public List<JsonMember> members() {
        return value.entrySet().stream()
                .map(e -> new JsonMember(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public void forEach(JsonTransform transform) {
        members().forEach(member -> transform.accept(null, member));
    }

    private static IllegalArgumentException typeMismatch(String attribute, String expected, JsonValue actual) {
        return new IllegalArgumentException(
                "Attribute '" + attribute + "' is not a JSON " + expected + ": " + actual.getClass().getSimpleName());
    }
}
