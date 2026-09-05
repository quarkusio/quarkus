package io.quarkus.observation.opentelemetry.context;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;

public class TypedAttributes {

    private final AttributesBuilder builder = Attributes.builder();

    public TypedAttributes put(AttributeKey<String> key, String value) {
        builder.put(key, value);
        return this;
    }

    public TypedAttributes put(AttributeKey<Long> key, long value) {
        builder.put(key, value);
        return this;
    }

    public TypedAttributes put(AttributeKey<Double> key, double value) {
        builder.put(key, value);
        return this;
    }

    public TypedAttributes put(AttributeKey<Boolean> key, boolean value) {
        builder.put(key, value);
        return this;
    }

    public Attributes build() {
        return builder.build();
    }
}
