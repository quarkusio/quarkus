package io.quarkus.bootstrap.json;

public interface JsonMultiValue extends JsonValue {
    default void forEach(JsonTransform transform) {
        transform.accept(null, this);
    }
}
