package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class ValueCreatorWrapper {

    private final String data;

    @JsonCreator
    public ValueCreatorWrapper(String data) {
        this.data = data;
    }

    @JsonValue
    public String getData() {
        return data;
    }
}
