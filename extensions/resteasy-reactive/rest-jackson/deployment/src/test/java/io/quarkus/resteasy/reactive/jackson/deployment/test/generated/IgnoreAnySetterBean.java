package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class IgnoreAnySetterBean {

    @JsonProperty("name")
    private String name;

    @JsonIgnore
    private String hidden;

    private Map<String, String> others = new HashMap<>();

    @JsonAnySetter
    public void addOther(String key, String value) {
        others.put(key, value);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHidden() {
        return hidden;
    }

    public void setHidden(String hidden) {
        this.hidden = hidden;
    }

    public Map<String, String> getOthers() {
        return others;
    }
}
