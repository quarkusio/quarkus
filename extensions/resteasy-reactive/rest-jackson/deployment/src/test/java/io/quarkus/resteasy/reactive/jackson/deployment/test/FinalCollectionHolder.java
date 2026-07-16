package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.ArrayList;
import java.util.List;

public class FinalCollectionHolder {

    private String name;
    private final List<String> items = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getItems() {
        return items;
    }
}
