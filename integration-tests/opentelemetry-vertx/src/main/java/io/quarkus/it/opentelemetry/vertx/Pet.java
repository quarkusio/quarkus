package io.quarkus.it.opentelemetry.vertx;

/**
 * Simple pojo.
 * The test using this pojo will use the generic codec facility.
 */
public class Pet {

    private final String name;

    private final String kind;

    public Pet(String name, String kind) {
        this.name = name;
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public String getKind() {
        return kind;
    }
}
