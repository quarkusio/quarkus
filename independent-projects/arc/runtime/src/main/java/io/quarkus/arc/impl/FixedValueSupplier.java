package io.quarkus.arc.impl;

import java.util.function.Supplier;

/**
 * {@link Supplier} implementation that supplies a pre-configured value.
 *
 * @author Maarten Mulders
 */
public class FixedValueSupplier<T> implements Supplier<T> {

    private final T value;

    public FixedValueSupplier(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return this.value;
    }
}
