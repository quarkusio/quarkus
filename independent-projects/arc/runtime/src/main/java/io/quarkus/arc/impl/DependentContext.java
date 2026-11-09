package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;

/**
 * The built-in context for {@link jakarta.enterprise.context.Dependent}.
 */
class DependentContext implements InjectableContext {
    @Override
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        Objects.requireNonNull(contextual, "Contextual must not be null");

        if (creationalContext == null) {
            // there's never an "existing instance" of a dependent-scoped bean
            return null;
        }

        T instance = contextual.create(creationalContext);
        CreationalContextImpl<T> cc = (CreationalContextImpl<T>) creationalContext;
        cc.addDependentInstance((InjectableBean<T>) contextual, instance, creationalContext);
        return instance;
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        return get(contextual, null);
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void destroy(Contextual<?> contextual) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ContextState getState() {
        throw new UnsupportedOperationException();
    }
}
