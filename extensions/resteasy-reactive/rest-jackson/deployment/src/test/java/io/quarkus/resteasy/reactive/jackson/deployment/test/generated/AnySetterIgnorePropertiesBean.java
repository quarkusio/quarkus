package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties({ "removed", "deleted" })
public class AnySetterIgnorePropertiesBean {

    @JsonProperty("id")
    private String identifier;

    private String name;

    private Map<String, Object> extras = new HashMap<>();

    @JsonAnySetter
    public void addExtra(String key, Object value) {
        extras.put(key, value);
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

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
