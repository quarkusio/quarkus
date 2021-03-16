package io.quarkus.funqy.runtime;

import io.quarkus.arc.runtime.BeanContainer;

public class FunctionConstructor<T> {
    public static volatile BeanContainer CONTAINER = null;

    protected volatile BeanContainer.Factory<T> factory;
    protected Class cls;

    public FunctionConstructor(Class<T> cls) {
        this.cls = cls;
    }

    public T construct() {
        if (factory == null)
            factory = CONTAINER.instanceFactory(cls);
        return factory.create().get();
    }
}
