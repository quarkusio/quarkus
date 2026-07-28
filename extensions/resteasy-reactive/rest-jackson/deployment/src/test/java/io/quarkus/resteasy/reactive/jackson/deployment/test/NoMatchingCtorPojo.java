package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.Objects;

public class NoMatchingCtorPojo {

    private final String name;
    private String description;

    public NoMatchingCtorPojo() {
        this.name = null;
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
        NoMatchingCtorPojo that = (NoMatchingCtorPojo) o;
        return Objects.equals(name, that.name) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description);
    }
}
