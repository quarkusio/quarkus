package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;

public class FieldAnySetterBean {

    private String name;

    @JsonAnySetter
    private Map<String, Object> extras = new LinkedHashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getExtras() {
        return extras;
    }
}
