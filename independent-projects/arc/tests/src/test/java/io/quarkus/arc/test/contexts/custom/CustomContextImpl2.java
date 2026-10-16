package io.quarkus.arc.test.contexts.custom;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

import io.quarkus.arc.InjectableContext;

public class CustomContextImpl2 implements InjectableContext {
    private final ThreadLocal<Map<Contextual<?>, ContextualInstance<?>>> current = ThreadLocal.withInitial(HashMap::new);

    @Override
    public Class<? extends Annotation> getScope() {
        return CustomScoped.class;
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        Map<Contextual<?>, ContextualInstance<?>> store = current.get();
        ContextualInstance<T> instance = (ContextualInstance<T>) store.get(contextual);
        if (instance == null && creationalContext != null) {
            instance = new ContextualInstance<T>(contextual.create(creationalContext), creationalContext, contextual);
            store.put(contextual, instance);
        }
        return instance != null ? instance.get() : null;
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
        Map<Contextual<?>, ContextualInstance<?>> store = current.get();
        ContextualInstance<?> contextualInstance = store.remove(contextual);
        if (contextualInstance != null) {
            contextualInstance.destroy();
        }
    }

    @Override
    public void destroy() {
        Map<Contextual<?>, ContextualInstance<?>> store = current.get();
        store.forEach((contextual, instance) -> {
            if (instance != null) {
                instance.destroy();
            }
        });
        store.clear();
    }

    @Override
    public ContextState getState() {
        throw new UnsupportedOperationException();
    }

    static final class ContextualInstance<T> {
        private final T value;
        private final CreationalContext<T> creationalContext;
        private final Contextual<T> contextual;

        ContextualInstance(T instance, CreationalContext<T> creationalContext, Contextual<T> contextual) {
            this.value = instance;
            this.creationalContext = creationalContext;
            this.contextual = contextual;
        }

        T get() {
            return value;
        }

        void destroy() {
            contextual.destroy(value, creationalContext);
        }
    }
}
