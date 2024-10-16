package io.quarkus.builder.json;

import io.quarkus.builder.JsonTransform;

public interface JsonMultiValue extends JsonValue {
    default void forEach(JsonTransform transform) {
        transform.accept(null, this);
    }
}
