package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CollectionSubclassWithProperty extends ArrayList<String> {

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
