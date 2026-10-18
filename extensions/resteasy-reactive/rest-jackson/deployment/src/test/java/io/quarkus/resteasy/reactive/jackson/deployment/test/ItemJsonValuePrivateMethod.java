package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class ItemJsonValuePrivateMethod {
    private final int value;

    @JsonCreator
    public ItemJsonValuePrivateMethod(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    @JsonValue
    private String format() {
        return Integer.toString(value);
    }
}
