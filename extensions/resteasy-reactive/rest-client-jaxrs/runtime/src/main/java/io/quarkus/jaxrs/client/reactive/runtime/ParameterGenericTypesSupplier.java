package io.quarkus.jaxrs.client.reactive.runtime;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.Supplier;

public class ParameterGenericTypesSupplier implements Supplier<Type[]> {

    private final Method method;
    private volatile Type[] value;

    public ParameterGenericTypesSupplier(Method method) {
        this.method = method;
    }

    @Override
    public Type[] get() {
        if (value == null) {
            value = method.getGenericParameterTypes();
        }
        return value;
    }
}
