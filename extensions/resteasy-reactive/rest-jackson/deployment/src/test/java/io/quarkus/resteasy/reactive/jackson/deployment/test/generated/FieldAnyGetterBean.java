package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class FieldAnyGetterBean {

    private String name;

    @JsonIgnore
    @JsonAnyGetter
    private Map<String, Object> additionalProperties = new LinkedHashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    @JsonAnySetter
    public void addProperty(String key, Object value) {
        additionalProperties.put(key, value);
    }
}
