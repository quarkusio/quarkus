package io.quarkus.it.mongodb.panache.bugs;

import java.util.Objects;

public final class Isbn {

    private final String value;

    private Isbn(String value) {
        Objects.requireNonNull(value);
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Isbn of(String value) {
        return new Isbn(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        return value.equals(((Isbn) o).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "ISBN{" + value + "}";
    }
}
