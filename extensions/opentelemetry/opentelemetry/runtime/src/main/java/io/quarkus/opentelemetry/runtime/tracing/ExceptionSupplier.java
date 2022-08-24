package io.quarkus.opentelemetry.runtime.tracing;

/**
 * A {@link java.util.function.Supplier} which allows to throw any kind of {@link Exception}
 *
 * @param <T> the type of results supplied by this supplier
 */
@FunctionalInterface
public interface ExceptionSupplier<T> {
    /**
     * Gets a result.
     *
     * @return a result
     */
    T get() throws Exception;
}
