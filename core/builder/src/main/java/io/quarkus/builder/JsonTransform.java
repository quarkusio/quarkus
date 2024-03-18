package io.quarkus.builder;

import java.util.function.Predicate;

@FunctionalInterface
public interface JsonTransform {
    void accept(Json.JsonBuilder<?> builder, JsonReader.JsonValue element);

    static JsonTransform dropping(Predicate<JsonReader.JsonValue> filter) {
        return (builder, element) -> {
            if (!filter.test(element))
                builder.with(element);
        };
    }
}
