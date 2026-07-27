package io.quarkus.runtime;

import java.util.Objects;

/**
 * Represents a proxyable object that can be returned from a bytecode recorder,
 * and passed between recorders.
 *
 * @deprecated Use {@code ActionBuilder} and its service dependency mechanism instead.
 *             Services can declare dependencies on other services directly, replacing
 *             the need for {@code RuntimeValue} as a parameter-passing mechanism.
 */
//@Deprecated(since = "4.0")
public class RuntimeValue<T> {

    private final T value;

    public RuntimeValue(T value) {
        Objects.requireNonNull(value);
        this.value = value;
    }

    public RuntimeValue() {
        this.value = null;
    }

    public T getValue() {
        if (value == null) {
            throw new IllegalStateException("Cannot call getValue() at deployment time");
        }
        return value;
    }
}
