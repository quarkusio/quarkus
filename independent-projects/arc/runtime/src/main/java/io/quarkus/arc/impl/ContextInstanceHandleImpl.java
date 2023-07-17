package io.quarkus.arc.impl;

import jakarta.enterprise.context.spi.CreationalContext;

import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.InjectableBean;

/**
 *
 *
 * @param <T>
 */
public class ContextInstanceHandleImpl<T> extends EagerInstanceHandle<T> implements ContextInstanceHandle<T> {

    public ContextInstanceHandleImpl(InjectableBean<T> bean, T instance, CreationalContext<T> creationalContext) {
        super(bean, instance, creationalContext);
    }

    @Override
    public void destroy() {
        destroyInternal();
    }

}
