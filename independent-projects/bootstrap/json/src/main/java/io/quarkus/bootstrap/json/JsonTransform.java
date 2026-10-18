package io.quarkus.bootstrap.json;

import java.util.function.Predicate;

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
