package io.quarkus.arc.impl;

import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.InjectableBean;
import javax.enterprise.context.spi.CreationalContext;

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
