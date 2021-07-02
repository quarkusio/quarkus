package io.quarkus.it.resteasy.reactive;

public class Pet {

    private String name;
    private String kind;

    public String getName() {
        return name;
    }

    public Pet setName(String name) {
        this.name = name;
        return this;
    }

    public String getKind() {
        return kind;
    }

    public Pet setKind(String kind) {
        this.kind = kind;
        return this;
    }
}
