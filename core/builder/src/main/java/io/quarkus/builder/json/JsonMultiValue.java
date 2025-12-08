package io.quarkus.builder.json;

import io.quarkus.builder.JsonTransform;

/**
 * @deprecated since 3.31.0 in favor of {@link io.quarkus.bootstrap.json.JsonMultiValue}
 */
@Deprecated(forRemoval = true)
public interface JsonMultiValue extends JsonValue {
    default void forEach(JsonTransform transform) {
        transform.accept(null, this);
    }
}
