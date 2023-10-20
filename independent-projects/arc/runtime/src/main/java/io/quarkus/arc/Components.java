package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import io.quarkus.arc.impl.ContextInstances;

public final class Components {

    private final Collection<InjectableBean<?>> beans;
    private final Supplier<Collection<RemovedBean>> removedBeans;
    private final Collection<InjectableObserverMethod<?>> observers;
    private final Collection<InjectableContext> contexts;
    private final Set<String> interceptorBindings;
    private final Map<Class<? extends Annotation>, Set<Annotation>> transitiveInterceptorBindings;
    private final Map<String, Set<String>> qualifierNonbindingMembers;
    private final Set<String> qualifiers;
    private final Map<Class<? extends Annotation>, Supplier<ContextInstances>> contextInstances;

    public Components(Collection<InjectableBean<?>> beans, Collection<InjectableObserverMethod<?>> observers,
            Collection<InjectableContext> contexts,
            Set<String> interceptorBindings,
            Map<Class<? extends Annotation>, Set<Annotation>> transitiveInterceptorBindings,
            Supplier<Collection<RemovedBean>> removedBeans, Map<String, Set<String>> qualifierNonbindingMembers,
            Set<String> qualifiers,
            Map<Class<? extends Annotation>, Supplier<ContextInstances>> contextInstances) {
        this.beans = beans;
        this.observers = observers;
        this.contexts = contexts;
        this.interceptorBindings = interceptorBindings;
        this.transitiveInterceptorBindings = transitiveInterceptorBindings;
        this.removedBeans = removedBeans;
        this.qualifierNonbindingMembers = qualifierNonbindingMembers;
        this.qualifiers = qualifiers;
        this.contextInstances = contextInstances;
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

    public Set<String> getInterceptorBindings() {
        return interceptorBindings;
    }

    public Map<Class<? extends Annotation>, Set<Annotation>> getTransitiveInterceptorBindings() {
        return transitiveInterceptorBindings;
    }

    public Supplier<Collection<RemovedBean>> getRemovedBeans() {
        return removedBeans;
    }

    /**
     * Values in the map are never null.
     *
     * @return a map of fully-qualified class names of all custom qualifiers to the set of non-binding members
     * @see jakarta.enterprise.util.Nonbinding
     */
    public Map<String, Set<String>> getQualifierNonbindingMembers() {
        return qualifierNonbindingMembers;
    }

    /**
     *
     * @return the set of fully-qualified class names of all registered qualifiers
     */
    public Set<String> getQualifiers() {
        return qualifiers;
    }

    public Map<Class<? extends Annotation>, Supplier<ContextInstances>> getContextInstances() {
        return contextInstances;
    }

}
