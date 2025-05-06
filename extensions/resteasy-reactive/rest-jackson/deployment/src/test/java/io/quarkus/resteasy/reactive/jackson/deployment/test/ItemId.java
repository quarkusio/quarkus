package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class ItemId {

    private final int value;

    @JsonCreator
    public ItemId(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    @JsonValue
    public String jsonValue() {
        return "" + value;
    }
}
