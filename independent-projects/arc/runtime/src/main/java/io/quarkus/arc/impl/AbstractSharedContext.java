package io.quarkus.arc.impl;

import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

abstract class AbstractSharedContext implements InjectableContext, InjectableContext.ContextState {

    protected final ComputingCache<String, ContextInstanceHandle<?>> instances;

    public AbstractSharedContext() {
        this.instances = new ComputingCache<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        InjectableBean<T> bean = (InjectableBean<T>) contextual;
        return (T) instances.computeIfAbsent(bean.getIdentifier(), new Supplier<ContextInstanceHandle<?>>() {
            @Override
            public ContextInstanceHandle<?> get() {
                return createInstanceHandle(bean, creationalContext);
            }
        }).get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Contextual<T> contextual) {
        InjectableBean<T> bean = (InjectableBean<T>) contextual;
        ContextInstanceHandle<?> handle = instances.getValueIfPresent(bean.getIdentifier());
        return handle != null ? (T) handle.get() : null;
    }

    @Override
    public ContextState getState() {
        return this;
    }

    @Override
    public Map<InjectableBean<?>, Object> getContextualInstances() {
        return instances.getPresentValues().stream()
                .collect(Collectors.toMap(ContextInstanceHandle::getBean, ContextInstanceHandle::get));
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void destroy(Contextual<?> contextual) {
        InjectableBean<?> bean = (InjectableBean<?>) contextual;
        ContextInstanceHandle<?> handle = instances.remove(bean.getIdentifier());
        if (handle != null) {
            handle.destroy();
        }
    }

    @Override
    public synchronized void destroy() {
        Set<ContextInstanceHandle<?>> values = instances.getPresentValues();
        // Destroy the producers first
        for (Iterator<ContextInstanceHandle<?>> iterator = values.iterator(); iterator.hasNext();) {
            ContextInstanceHandle<?> instanceHandle = iterator.next();
            if (instanceHandle.getBean().getDeclaringBean() != null) {
                instanceHandle.destroy();
                iterator.remove();
            }
        }
        for (ContextInstanceHandle<?> instanceHandle : values) {
            instanceHandle.destroy();
        }
        instances.clear();
    }

    @Override
    public void destroy(ContextState state) {
        if (state == this) {
            destroy();
        } else {
            throw new IllegalArgumentException("Invalid state: " + state.getClass().getName());
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T> ContextInstanceHandle createInstanceHandle(InjectableBean<T> bean,
            CreationalContext<T> creationalContext) {
        return new ContextInstanceHandleImpl(bean, bean.create(creationalContext), creationalContext);
    }

}
