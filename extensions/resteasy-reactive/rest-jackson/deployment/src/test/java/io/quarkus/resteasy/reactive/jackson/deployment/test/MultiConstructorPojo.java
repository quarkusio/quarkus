package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.Objects;

public class MultiConstructorPojo {

    private final String name;
    private String description;

    public MultiConstructorPojo() {
        this.name = null;
    }

    public MultiConstructorPojo(String name) {
        this.name = name;
    }

    public MultiConstructorPojo(long x, long y) {
        this.name = String.valueOf(x + y);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        MultiConstructorPojo that = (MultiConstructorPojo) o;
        return Objects.equals(name, that.name) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description);
    }
}
