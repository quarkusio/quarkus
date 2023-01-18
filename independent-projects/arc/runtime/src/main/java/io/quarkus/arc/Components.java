package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class Components {

    private final Collection<InjectableBean<?>> beans;
    private final Supplier<Collection<RemovedBean>> removedBeans;
    private final Collection<InjectableObserverMethod<?>> observers;
    private final Collection<InjectableContext> contexts;
    private final Map<Class<? extends Annotation>, Set<Annotation>> transitiveInterceptorBindings;
    private final Map<String, Set<String>> qualifierNonbindingMembers;
    private final Set<String> qualifiers;

    public Components(Collection<InjectableBean<?>> beans, Collection<InjectableObserverMethod<?>> observers,
            Collection<InjectableContext> contexts,
            Map<Class<? extends Annotation>, Set<Annotation>> transitiveInterceptorBindings,
            Supplier<Collection<RemovedBean>> removedBeans, Map<String, Set<String>> qualifierNonbindingMembers,
            Set<String> qualifiers) {
        this.beans = beans;
        this.observers = observers;
        this.contexts = contexts;
        this.transitiveInterceptorBindings = transitiveInterceptorBindings;
        this.removedBeans = removedBeans;
        this.qualifierNonbindingMembers = qualifierNonbindingMembers;
        this.qualifiers = qualifiers;
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

}
