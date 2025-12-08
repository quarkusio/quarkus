package io.quarkus.builder.json;

import java.util.List;
import java.util.stream.Stream;

import io.quarkus.builder.JsonTransform;

/**
 * @deprecated since 3.31.0 in favor of {@link io.quarkus.bootstrap.json.JsonArray}
 */
@Deprecated(forRemoval = true)
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

    @Override
    public void forEach(JsonTransform transform) {
        value.forEach(v -> transform.accept(null, v));
    }
}
