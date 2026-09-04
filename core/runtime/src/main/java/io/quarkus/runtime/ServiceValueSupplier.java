package io.quarkus.runtime;

import java.util.function.Supplier;

/**
 * A {@link Supplier} that wraps a service value for backward compatibility
 * with recorder-based consumers that expect {@code Supplier<T>}.
 * <p>
 * This is a temporary bridge used during the recorder-to-service coexistence
 * period. It will be removed once all consumers are converted to use
 * {@code require()} on the service directly.
 *
 * @param <T> the service value type
 */
public final class ServiceValueSupplier<T> implements Supplier<T> {
    private final T value;

    private ServiceValueSupplier(T value) {
        this.value = value;
    }

    /**
     * Create a supplier wrapping the given value.
     *
     * @param value the service value
     * @return a supplier that returns the value
     * @param <T> the value type
     */
    @SuppressWarnings("unchecked")
    public static <T> Supplier<T> of(Object value) {
        return new ServiceValueSupplier<>((T) value);
    }

    @Override
    public T get() {
        return value;
    }
}
