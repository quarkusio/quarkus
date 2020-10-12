package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.Managed;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

/**
 * The built-in context for {@link Managed}.
 *
 */
class ManagedContext implements InjectableContext {

    @Override
    public void destroy(Contextual<?> contextual) {

    }

    @Override
    public Class<? extends Annotation> getScope() {
        return Managed.class;
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        return contextual.create(creationalContext);
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        return null;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void destroy() {

    }

    @Override
    public ContextState getState() {
        return new ContextState() {
            @Override
            public Map<InjectableBean<?>, Object> getContextualInstances() {
                return Collections.emptyMap();
            }
        };
    }
}
