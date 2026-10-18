package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestWithJsonPropertyDto {

    @JsonProperty("name")
    private final String field;

    public TestWithJsonPropertyDto(String field) {
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
