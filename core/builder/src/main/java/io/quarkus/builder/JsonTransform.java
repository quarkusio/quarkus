package io.quarkus.builder;

import java.util.function.Predicate;

import io.quarkus.builder.json.JsonValue;

/**
 * @deprecated since 3.31.0 in favor of io.quarkus.bootstrap.json.JsonTransformer
 */
@Deprecated(forRemoval = true)
@FunctionalInterface
public interface JsonTransform {
    void accept(Json.JsonBuilder<?> builder, JsonValue element);

    static JsonTransform dropping(Predicate<JsonValue> filter) {
        return (builder, element) -> {
            if (!filter.test(element))
                builder.add(element);
        };
    }
}
