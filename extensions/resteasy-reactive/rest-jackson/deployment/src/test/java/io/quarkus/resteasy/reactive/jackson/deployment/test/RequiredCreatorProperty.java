package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RequiredCreatorProperty {

    private String name;
    private String value;

    public RequiredCreatorProperty() {
    }

    @JsonCreator
    public RequiredCreatorProperty(
            @JsonProperty(value = "name", required = true) String name,
            @JsonProperty(value = "value") String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
