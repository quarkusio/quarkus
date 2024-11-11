package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.Map;

public class MapWrapper {

    private String name;
    private Map<String, String> properties;

    public MapWrapper() {
    }

    public MapWrapper(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
