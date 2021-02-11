package io.quarkus.vertx;

public class Pet {

    private final String kind;
    private final String name;

    public Pet(String name, String kind) {
        this.kind = kind;
        this.name = name;
    }

    public String getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }
}
