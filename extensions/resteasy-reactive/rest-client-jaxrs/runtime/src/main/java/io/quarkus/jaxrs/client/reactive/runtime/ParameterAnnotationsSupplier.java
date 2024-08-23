package io.quarkus.jaxrs.client.reactive.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.function.Supplier;

public class ParameterAnnotationsSupplier implements Supplier<Annotation[][]> {

    private final Method method;
    private volatile Annotation[][] value;

    public ParameterAnnotationsSupplier(Method method) {
        this.method = method;
    }

    @Override
    public Annotation[][] get() {
        if (value == null) {
            value = method.getParameterAnnotations();
        }
        return value;
    }
}
