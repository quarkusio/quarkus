package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MapSubclassWithProperty extends HashMap<String, Object> {

    private String declared;

    @JsonProperty("declared")
    public String getDeclared() {
        return declared;
    }

    @JsonProperty("declared")
    public void setDeclared(String declared) {
        this.declared = declared;
    }
}
