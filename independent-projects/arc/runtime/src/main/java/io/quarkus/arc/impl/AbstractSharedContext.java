package io.quarkus.arc.impl;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;

abstract class AbstractSharedContext implements InjectableContext, InjectableContext.ContextState {

    protected final ContextInstances instances;

    public AbstractSharedContext() {
        this(new ComputingCacheContextInstances());
    }

    public AbstractSharedContext(ContextInstances instances) {
        this.instances = Objects.requireNonNull(instances);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        Objects.requireNonNull(contextual, "Contextual must not be null");
        Objects.requireNonNull(creationalContext, "CreationalContext must not be null");
        InjectableBean<T> bean = (InjectableBean<T>) contextual;
        if (!Scopes.scopeMatches(this, bean)) {
            throw Scopes.scopeDoesNotMatchException(this, bean);
        }
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
        Objects.requireNonNull(contextual, "Contextual must not be null");
        InjectableBean<T> bean = (InjectableBean<T>) contextual;
        if (!Scopes.scopeMatches(this, bean)) {
            throw Scopes.scopeDoesNotMatchException(this, bean);
        }
        ContextInstanceHandle<?> handle = instances.getIfPresent(bean.getIdentifier());
        return handle != null ? (T) handle.get() : null;
    }

    @Override
    public ContextState getState() {
        return this;
    }

    @Override
    public ContextState getStateIfActive() {
        return this;
    }

    @Override
    public Map<InjectableBean<?>, Object> getContextualInstances() {
        return instances.getAllPresent().stream()
                .collect(Collectors.toUnmodifiableMap(ContextInstanceHandle::getBean, ContextInstanceHandle::get));
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
        List<ContextInstanceHandle<?>> values = new LinkedList<>();
        instances.forEach(values::add);
        if (values.isEmpty()) {
            return;
        }
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
