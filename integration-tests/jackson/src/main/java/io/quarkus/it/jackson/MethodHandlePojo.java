package io.quarkus.it.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class MethodHandlePojo {

    private final String creatorValue;

    @JsonProperty
    private String fieldValue;

    private String setterValue;

    private MethodHandlePojo(String creatorValue) {
        this.creatorValue = creatorValue;
    }

    @JsonCreator
    public static MethodHandlePojo create(@JsonProperty("creatorValue") String creatorValue) {
        return new MethodHandlePojo(creatorValue);
    }

    public String getCreatorValue() {
        return creatorValue;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public String getSetterValue() {
        return setterValue;
    }

    public void setSetterValue(String setterValue) {
        this.setterValue = setterValue;
    }
}
