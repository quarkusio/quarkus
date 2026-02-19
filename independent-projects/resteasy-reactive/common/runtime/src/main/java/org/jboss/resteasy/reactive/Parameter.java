package org.jboss.resteasy.reactive;

import java.util.Objects;

public class Parameter<T> {
    private final T value;
    private final boolean set;

    private Parameter(T value, boolean set) {
        if (value != null && !set) {
            throw new IllegalArgumentException("Cannot be not-set and have a value: " + value);
        }
        this.value = value;
        this.set = set;
    }

    public static <T> Parameter<T> absent() {
        return new Parameter<>(null, false);
    }

    public static <T> Parameter<T> cleared() {
        return new Parameter<>(null, true);
    }

    // FIXME: find better name
    public static <T> Parameter<T> ofNullable(T value) {
        return new Parameter<>(value, true);
    }

    public static <T> Parameter<T> set(T value) {
        Objects.requireNonNull(value, "Parameter value cannot be null");
        if (value instanceof String string) {
            if (string.isEmpty()) {
                throw new IllegalArgumentException("Parameter value cannot be an empty string");
            }
        }
        return new Parameter<>(value, true);
    }

    public boolean isAbsent() {
        return set == false;
    }

    public boolean isCleared() {
        return set == true && value == null;
    }

    public boolean isSet() {
        return set == true && value != null;
    }

    public T getValue() {
        return value;
    }
}
