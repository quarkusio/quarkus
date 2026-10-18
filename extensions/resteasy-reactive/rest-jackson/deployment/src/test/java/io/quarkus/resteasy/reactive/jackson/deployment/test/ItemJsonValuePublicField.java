package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class ItemJsonValuePublicField {
    @JsonValue
    public final int value;

    @JsonCreator
    public ItemJsonValuePublicField(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}
