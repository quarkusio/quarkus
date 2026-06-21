package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class AnyGetterBean {

    private String name;

    private Map<String, String> properties = new HashMap<>();

    @JsonAnyGetter
    public Map<String, String> getProperties() {
        return properties;
    }

    @JsonAnySetter
    public void addProperty(String key, String value) {
        properties.put(key, value);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
