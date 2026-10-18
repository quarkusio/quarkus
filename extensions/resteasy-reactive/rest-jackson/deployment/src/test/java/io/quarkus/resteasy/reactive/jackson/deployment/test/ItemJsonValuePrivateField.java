package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class ItemJsonValuePrivateField {
    @JsonValue
    private final int value;

    @JsonCreator
    public ItemJsonValuePrivateField(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}
