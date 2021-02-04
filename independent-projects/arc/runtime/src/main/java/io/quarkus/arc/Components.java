package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class Components {

    private final Collection<InjectableBean<?>> beans;
    private final Collection<RemovedBean> removedBeans;
    private final Collection<InjectableObserverMethod<?>> observers;
    private final Collection<InjectableContext> contexts;
    private final Map<Class<? extends Annotation>, Set<Annotation>> transitiveInterceptorBindings;
    private final Map<String, Set<String>> qualifierNonbindingMembers;

    public Components(Collection<InjectableBean<?>> beans, Collection<InjectableObserverMethod<?>> observers,
            Collection<InjectableContext> contexts,
            Map<Class<? extends Annotation>, Set<Annotation>> transitiveInterceptorBindings) {
        this(beans, observers, contexts, transitiveInterceptorBindings, Collections.emptyList(), Collections.emptyMap());
    }

    public Components(Collection<InjectableBean<?>> beans, Collection<InjectableObserverMethod<?>> observers,
            Collection<InjectableContext> contexts,
            Map<Class<? extends Annotation>, Set<Annotation>> transitiveInterceptorBindings,
            Collection<RemovedBean> removedBeans, Map<String, Set<String>> qualifierNonbindingMembers) {
        this.beans = beans;
        this.observers = observers;
        this.contexts = contexts;
        this.transitiveInterceptorBindings = transitiveInterceptorBindings;
        this.removedBeans = removedBeans;
        this.qualifierNonbindingMembers = qualifierNonbindingMembers;
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

    public Collection<RemovedBean> getRemovedBeans() {
        return removedBeans;
    }

    public Map<String, Set<String>> getQualifierNonbindingMembers() {
        return qualifierNonbindingMembers;
    }

}
