package io.quarkus.vertx.http.runtime.security;

import java.util.function.Supplier;

public class SupplierImpl<T> implements Supplier<T> {

    T value;

    public SupplierImpl() {
    }

    public SupplierImpl(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public SupplierImpl<T> setValue(T value) {
        this.value = value;
        return this;
    }

    @Override
    public T get() {
        return value;
    }
}
