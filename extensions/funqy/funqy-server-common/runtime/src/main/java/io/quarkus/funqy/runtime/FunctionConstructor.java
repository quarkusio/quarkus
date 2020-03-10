package io.quarkus.funqy.runtime;

import io.quarkus.arc.runtime.BeanContainer;

public class FunctionConstructor {
    public static volatile BeanContainer CONTAINER = null;

    protected volatile BeanContainer.Factory<?> factory;
    protected Class cls;

    public FunctionConstructor(Class cls) {
        this.cls = cls;
    }

    public Object construct() {
        if (factory == null)
            factory = CONTAINER.instanceFactory(cls);
        return factory.create().get();
    }
}
