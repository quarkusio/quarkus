package io.quarkus.arc.impl;

import jakarta.enterprise.context.spi.CreationalContext;

import org.jboss.logging.Logger;

import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.InjectableBean;

/**
 *
 *
 * @param <T>
 */
public class ContextInstanceHandleImpl<T> extends EagerInstanceHandle<T> implements ContextInstanceHandle<T> {

    private static final Logger LOG = Logger.getLogger(ContextInstanceHandleImpl.class);

    public ContextInstanceHandleImpl(InjectableBean<T> bean, T instance, CreationalContext<T> creationalContext) {
        super(bean, instance, creationalContext);
    }

    @Override
    public void destroy() {
        try {
            destroyInternal();
        } catch (Exception e) {
            LOG.error("Unable to destroy instance" + get(), e);
        }
    }

}
