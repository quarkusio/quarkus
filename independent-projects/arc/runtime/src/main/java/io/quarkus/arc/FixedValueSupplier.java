package io.quarkus.arc;

import java.util.function.Supplier;

/**
 * {@link Supplier} implementation that supplies a pre-configured value.
 *
 * @author Maarten Mulders
 */
public class FixedValueSupplier implements Supplier {
    private final Object value;

    public FixedValueSupplier(Object value) {
        this.value = value;
    }

    @Override
    public Object get() {
        return this.value;
    }
}
