package io.quarkus.arc.impl;

import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

abstract class AbstractSharedContext implements InjectableContext, InjectableContext.ContextState {

    private final ComputingCache<Key<?>, ContextInstanceHandle<?>> instances;

    public AbstractSharedContext() {
        this.instances = new ComputingCache<>(AbstractSharedContext::createInstanceHandle);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        return (T) instances.getValue(new Key<>(contextual, creationalContext)).get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Contextual<T> contextual) {
        ContextInstanceHandle<?> handle = instances.getValueIfPresent(new Key<>(contextual, null));
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
        ContextInstanceHandle<?> handle = instances.remove(new Key<>(contextual, null));
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
    private static ContextInstanceHandle createInstanceHandle(Key key) {
        InjectableBean<?> bean = (InjectableBean<?>) key.contextual;
        return new ContextInstanceHandleImpl(bean, bean.create(key.creationalContext), key.creationalContext);
    }

    private static final class Key<T> {

        private final Contextual<T> contextual;
        private final CreationalContext<T> creationalContext;

        Key(Contextual<T> contextual, CreationalContext<T> creationalContext) {
            this.contextual = Objects.requireNonNull(contextual);
            this.creationalContext = creationalContext;
        }

        @Override
        public int hashCode() {
            return contextual.hashCode();
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Key)) {
                return false;
            }
            Key other = (Key) obj;
            // Shortcut removes hotspot on contextual.equals
            if (contextual == other.contextual) {
                return true;
            }
            if (!contextual.equals(other.contextual)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Key for " + contextual;
        }

    }

}
