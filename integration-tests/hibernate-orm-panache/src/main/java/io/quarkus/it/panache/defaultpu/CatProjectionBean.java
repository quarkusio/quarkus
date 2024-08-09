package io.quarkus.it.panache.defaultpu;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CatProjectionBean {

    private final String name;

    private final String ownerName;

    private final Double weight;

    public CatProjectionBean(String name, String ownerName) {
        this(name, ownerName, null);
    }

    public CatProjectionBean(String name, String ownerName, Double weight) {
        this.name = name;
        this.ownerName = ownerName;
        this.weight = weight;
    }

    public String getName() {
        return name;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Double getWeight() {
        return weight;
    }
}
