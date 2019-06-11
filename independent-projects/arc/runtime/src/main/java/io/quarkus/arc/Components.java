package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public final class Components {

    private final Collection<InjectableBean<?>> beans;
    private final Collection<InjectableObserverMethod<?>> observers;
    private final Collection<InjectableContext> contexts;
    private final Map<Class<? extends Annotation>, Set<Annotation>> transitiveInterceptorBindings;

    public Components(Collection<InjectableBean<?>> beans, Collection<InjectableObserverMethod<?>> observers,
            Collection<InjectableContext> contexts,
            Map<Class<? extends Annotation>, Set<Annotation>> transitiveInterceptorBindings) {
        this.beans = beans;
        this.observers = observers;
        this.contexts = contexts;
        this.transitiveInterceptorBindings = transitiveInterceptorBindings;
    }

    public Collection<InjectableBean<?>> getBeans() {
        return beans;
    }

    public Collection<InjectableObserverMethod<?>> getObservers() {
        return observers;
    }

    public Collection<InjectableContext> getContexts() {
        return contexts;
    }

    public Map<Class<? extends Annotation>, Set<Annotation>> getTransitiveInterceptorBindings() {
        return transitiveInterceptorBindings;
    }

}
