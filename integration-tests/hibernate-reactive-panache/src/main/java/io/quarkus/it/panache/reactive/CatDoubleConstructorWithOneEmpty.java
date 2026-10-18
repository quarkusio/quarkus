package io.quarkus.it.panache.reactive;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CatDoubleConstructorWithOneEmpty {

    public String name;

    public String ownerName;

    @SuppressWarnings("unused")
    public CatDoubleConstructorWithOneEmpty() {
    }

    @SuppressWarnings("unused")
    public CatDoubleConstructorWithOneEmpty(String name) {
        this.name = name;
    }

    @SuppressWarnings("unused")
    public CatDoubleConstructorWithOneEmpty(String name, String ownerName) {
        this.name = name;
        this.ownerName = ownerName;
    }
}
