package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MyObject {

    private String name;

    private String description;

    private Map<String, Integer> map = new HashMap<>();

    private String[] strings = new String[0];

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Map<String, Integer> getMap() {
        return map;
    }

    public void setMap(Map<String, Integer> map) {
        this.map = map;
    }

    public String[] getStrings() {
        return strings;
    }

    public void setStrings(String... strings) {
        this.strings = strings;
    }
}
