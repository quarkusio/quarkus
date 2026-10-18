package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/// Tests that `@JsonAnySetter` on a 2-param method triggers the safety valve fallback.
/// The common pattern (non-`set*` name, 2 params) is invisible to `isSetterMethod()`.
public class AnySetterRequest {
    @JsonProperty("known")
    private String known;

    private final Map<String, Object> extras = new HashMap<>();

    public String getKnown() {
        return known;
    }

    public void setKnown(String known) {
        this.known = known;
    }

    @JsonAnySetter
    public void handleUnknown(String key, Object value) {
        extras.put(key, value);
    }

    public Map<String, Object> getExtras() {
        return extras;
    }
}
