package io.quarkus.it.panache.reactive;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CatProjectionBean {

    public final String name;

    public final String ownerName;

    public final Double weight;

    public CatProjectionBean(String name, String ownerName) {
        this(name, ownerName, null);
    }

    public CatProjectionBean(String name, String ownerName, Double weight) {
        this.name = name;
        this.ownerName = ownerName;
        this.weight = weight;
    }
}
