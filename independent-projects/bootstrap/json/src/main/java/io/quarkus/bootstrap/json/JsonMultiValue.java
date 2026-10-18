package io.quarkus.bootstrap.json;

public sealed interface JsonMultiValue extends JsonValue permits JsonArray, JsonObject {
    default void forEach(JsonTransform transform) {
        transform.accept(null, this);
    }
}
