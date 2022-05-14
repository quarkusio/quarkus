package io.quarkus.opentelemetry.runtime.tracing;

@FunctionalInterface
public interface ExceptionSupplier<T> {

    T get() throws Exception;

}
