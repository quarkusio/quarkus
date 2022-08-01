package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;
import java.util.function.Consumer;
import javax.enterprise.context.spi.CreationalContext;

/**
 *
 * @param <T>
 */
class EagerInstanceHandle<T> extends AbstractInstanceHandle<T> {

    @SuppressWarnings("unchecked")
    public static final <T> InstanceHandle<T> unavailable() {
        return (InstanceHandle<T>) UNAVAILABLE;
    }

    static final EagerInstanceHandle<Object> UNAVAILABLE = new EagerInstanceHandle<Object>(null, null, null, null, null);

    private final T instance;

    EagerInstanceHandle(InjectableBean<T> bean, T instance, CreationalContext<T> creationalContext) {
        this(bean, instance, creationalContext, null, null);
    }

    EagerInstanceHandle(InjectableBean<T> bean, T instance, CreationalContext<T> creationalContext,
            CreationalContext<?> parentCreationalContext, Consumer<T> destroyLogic) {
        super(bean, creationalContext, parentCreationalContext, destroyLogic);
        this.instance = instance;
    }

    @Override
    protected boolean isInstanceCreated() {
        return true;
    }

    @Override
    protected T instanceInternal() {
        return instance;
    }

}
