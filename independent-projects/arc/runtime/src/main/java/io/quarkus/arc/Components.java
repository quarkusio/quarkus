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
    private final Map<String, Set<String>> interceptorBindingNonbindingMembers;
    private final Map<Class<? extends Annotation>, Set<Annotation>> transitiveInterceptorBindings;
    private final Set<String> qualifiers;
    private final Map<String, Set<String>> qualifierNonbindingMembers;
    private final Map<Class<? extends Annotation>, Supplier<ContextInstances>> contextInstances;

    public Components(Collection<InjectableBean<?>> beans,
            Collection<InjectableObserverMethod<?>> observers,
            Collection<InjectableContext> contexts,
            Set<String> interceptorBindings,
            Map<String, Set<String>> interceptorBindingNonbindingMembers,
            Map<Class<? extends Annotation>, Set<Annotation>> transitiveInterceptorBindings,
            Supplier<Collection<RemovedBean>> removedBeans,
            Set<String> qualifiers,
            Map<String, Set<String>> qualifierNonbindingMembers,
            Map<Class<? extends Annotation>, Supplier<ContextInstances>> contextInstances) {
        this.beans = beans;
        this.observers = observers;
        this.contexts = contexts;
        this.interceptorBindings = interceptorBindings;
        this.interceptorBindingNonbindingMembers = interceptorBindingNonbindingMembers;
        this.transitiveInterceptorBindings = transitiveInterceptorBindings;
        this.removedBeans = removedBeans;
        this.qualifiers = qualifiers;
        this.qualifierNonbindingMembers = qualifierNonbindingMembers;
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

    public Map<String, Set<String>> getInterceptorBindingNonbindingMembers() {
        return interceptorBindingNonbindingMembers;
    }

    public Map<Class<? extends Annotation>, Set<Annotation>> getTransitiveInterceptorBindings() {
        return transitiveInterceptorBindings;
    }

    public Supplier<Collection<RemovedBean>> getRemovedBeans() {
        return removedBeans;
    }

    /**
     *
     * @return the set of fully-qualified class names of all qualifiers
     */
    public Set<String> getQualifiers() {
        return qualifiers;
    }

    /**
     * Values in the map are never null.
     *
     * @return a map of fully-qualified class names of all qualifiers to the set of their non-binding members
     * @see jakarta.enterprise.util.Nonbinding
     */
    public Map<String, Set<String>> getQualifierNonbindingMembers() {
        return qualifierNonbindingMembers;
    }

    public Map<Class<? extends Annotation>, Supplier<ContextInstances>> getContextInstances() {
        return contextInstances;
    }

}
