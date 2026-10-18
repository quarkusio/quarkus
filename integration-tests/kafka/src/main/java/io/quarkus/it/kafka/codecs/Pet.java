package io.quarkus.it.kafka.codecs;

public class Pet {

    private String kind;

    private String name;

    public String getKind() {
        return kind;
    }

    public Pet setKind(String kind) {
        this.kind = kind;
        return this;
    }

    public String getName() {
        return name;
    }

    public Pet setName(String name) {
        this.name = name;
        return this;
    }
}
