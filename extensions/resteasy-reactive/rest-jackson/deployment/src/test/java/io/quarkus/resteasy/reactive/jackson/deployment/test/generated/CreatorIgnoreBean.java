package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreatorIgnoreBean {

    private final String name;
    private final int value;

    @JsonIgnore
    private final String computed;

    @JsonCreator
    public CreatorIgnoreBean(
            @JsonProperty("name") String name,
            @JsonProperty("value") int value) {
        this.name = name;
        this.value = value;
        this.computed = name + ":" + value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public String getComputed() {
        return computed;
    }
}
