package io.quarkus.arc.runtime;

import java.util.function.Supplier;

import io.quarkus.arc.Arc;

public class BeanLookupSupplier implements Supplier<Object> {

    private Class<?> type;

    public BeanLookupSupplier() {
    }

    public BeanLookupSupplier(Class<?> type) {
        this.type = type;
    }

    public Class<?> getType() {
        return type;
    }

    public BeanLookupSupplier setType(Class<?> type) {
        this.type = type;
        return this;
    }

    @Override
    public Object get() {
        return Arc.container().instance(type).get();
    }
}
