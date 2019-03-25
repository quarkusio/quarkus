/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.arc;

import io.quarkus.arc.ArcCDIProvider.ArcCDI;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Singleton;
import org.jboss.logging.Logger;

/**
 *
 * @author Martin Kouba
 */
class ArcContainerImpl implements ArcContainer {

    private static final Logger LOGGER = Logger.getLogger(ArcContainerImpl.class.getPackage().getName());

    private final String id;

    private final AtomicBoolean running;

    private final List<InjectableBean<?>> beans;
    private final List<InjectableInterceptor<?>> interceptors;
    private final List<InjectableObserverMethod<?>> observers;

    // List of "ambiguous" contexts that could share a scope
    private final List<InjectableContext> contexts;
    private final ManagedContext requestContext;
    private final InjectableContext applicationContext;
    private final InjectableContext singletonContext;

    private final ComputingCache<Resolvable, Set<InjectableBean<?>>> resolved;
    private final ComputingCache<String, InjectableBean<?>> beansById;
    private final ComputingCache<String, Set<InjectableBean<?>>> beansByName;

    private final List<ResourceReferenceProvider> resourceProviders;

    public ArcContainerImpl() {
        id = UUID.randomUUID().toString();
        running = new AtomicBoolean(true);
        beans = new ArrayList<>();
        interceptors = new ArrayList<>();
        observers = new ArrayList<>();

        applicationContext = new ApplicationContext();
        singletonContext = new SingletonContext();
        requestContext = new RequestContext();
        contexts = new ArrayList<>();
        contexts.add(requestContext);

        for (ComponentsProvider componentsProvider : ServiceLoader.load(ComponentsProvider.class)) {
            Components components = componentsProvider.getComponents();
            for (InjectableBean<?> bean : components.getBeans()) {
                if (bean instanceof InjectableInterceptor) {
                    interceptors.add((InjectableInterceptor<?>) bean);
                } else {
                    beans.add(bean);
                }
            }
            observers.addAll(components.getObservers());
            // Add custom contexts
            for (InjectableContext context : components.getContexts()) {
                if (ApplicationScoped.class.equals(context.getScope())) {
                    throw new IllegalStateException(
                            "Failed to register a context - built-in application context is always active: " + context);
                }
                if (Singleton.class.equals(context.getScope())) {
                    throw new IllegalStateException(
                            "Failed to register a context - built-in singleton context is always active: " + context);
                }
                contexts.add(context);
            }
        }
        Collections.sort(interceptors, (i1, i2) -> Integer.compare(i2.getPriority(), i1.getPriority()));

        resolved = new ComputingCache<>(this::resolve);
        beansById = new ComputingCache<>(this::findById);
        beansByName = new ComputingCache<>(this::resolve);
        resourceProviders = new ArrayList<>();
        for (ResourceReferenceProvider resourceProvider : ServiceLoader.load(ResourceReferenceProvider.class)) {
            resourceProviders.add(resourceProvider);
        }
    }

    void init() {
        requireRunning();
        // Fire an event with qualifier @Initialized(ApplicationScoped.class)
        Set<Annotation> qualifiers = new HashSet<>(4);
        qualifiers.add(Initialized.Literal.APPLICATION);
        qualifiers.add(Any.Literal.INSTANCE);
        EventImpl.createNotifier(Object.class, Object.class, qualifiers, this).notify(toString());
        // Configure CDIProvider used for CDI.current()
        CDI.setCDIProvider(new ArcCDIProvider());
        LOGGER.debugf("ArC DI container initialized [beans=%s, observers=%s]", beans.size(), observers.size());
    }

    @Override
    public InjectableContext getActiveContext(Class<? extends Annotation> scopeType) {
        requireRunning();
        // Application/Singleton context is always active
        if (ApplicationScoped.class.equals(scopeType)) {
            return applicationContext;
        } else if (Singleton.class.equals(scopeType)) {
            return singletonContext;
        }
        List<InjectableContext> active = new ArrayList<>();
        for (InjectableContext context : contexts) {
            if (scopeType.equals(context.getScope()) && context.isActive()) {
                active.add(context);
            }
        }
        if (active.isEmpty()) {
            return null;
        } else if (active.size() == 1) {
            return active.get(0);
        }
        throw new IllegalArgumentException("More than one context object for the given scope: " + active);
    }

    @Override
    public Set<Class<? extends Annotation>> getScopes() {
        Set<Class<? extends Annotation>> scopes = contexts.stream().map(InjectableContext::getScope)
                .collect(Collectors.toSet());
        scopes.add(ApplicationScoped.class);
        scopes.add(Singleton.class);
        return scopes;
    }

    @Override
    public <T> InstanceHandle<T> instance(Class<T> type, Annotation... qualifiers) {
        requireRunning();
        return instanceHandle(type, qualifiers);
    }

    @Override
    public <T> InstanceHandle<T> instance(TypeLiteral<T> type, Annotation... qualifiers) {
        requireRunning();
        return instanceHandle(type.getType(), qualifiers);
    }

    @Override
    public <X> InstanceHandle<X> instance(Type type, Annotation... qualifiers) {
        requireRunning();
        return instanceHandle(type, qualifiers);
    }

    @Override
    public <T> Supplier<InstanceHandle<T>> instanceSupplier(Class<T> type, Annotation... qualifiers) {
        requireRunning();

        InjectableBean<T> bean = getBean(type, qualifiers);
        if (bean == null) {
            return null;
        }
        return new Supplier<InstanceHandle<T>>() {
            @Override
            public InstanceHandle<T> get() {
                return beanInstanceHandle(bean, null);
            }
        };
    }

    @Override
    public <T> InstanceHandle<T> instance(InjectableBean<T> bean) {
        Objects.requireNonNull(bean);
        requireRunning();
        return bean != null ? (InstanceHandle<T>) beanInstanceHandle(bean, null) : InstanceHandleImpl.unavailable();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> InjectableBean<T> bean(String beanIdentifier) {
        Objects.requireNonNull(beanIdentifier);
        requireRunning();
        return (InjectableBean<T>) beansById.getValue(beanIdentifier);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> InstanceHandle<T> instance(String name) {
        Objects.requireNonNull(name);
        requireRunning();
        Set<InjectableBean<?>> resolvedBeans = beansByName.getValue(name);
        return resolvedBeans.isEmpty() || resolvedBeans.size() > 1 ? InstanceHandleImpl.unavailable()
                : (InstanceHandle<T>) beanInstanceHandle(resolvedBeans.iterator()
                        .next(), null);
    }

    @Override
    public ManagedContext requestContext() {
        requireRunning();
        return requestContext;
    }

    @Override
    public BeanManager beanManager() {
        return BeanManagerImpl.INSTANCE.get();
    }

    @Override
    public String toString() {
        return "ArcContainerImpl [id=" + id + ", running=" + running + ", beans=" + beans.size() + ", observers="
                + observers.size() + ", scopes="
                + getScopes() + "]";
    }

    synchronized void shutdown() {
        if (running.get()) {
            // Make sure all dependent bean instances obtained via CDI.current() are destroyed correctly
            CDI<?> cdi = CDI.current();
            if (cdi instanceof ArcCDI) {
                ArcCDI arcCdi = (ArcCDI) cdi;
                arcCdi.destroy();
            }
            // Fire an event with qualifier @BeforeDestroyed(ApplicationScoped.class)
            Set<Annotation> beforeDestroyQualifiers = new HashSet<>(4);
            beforeDestroyQualifiers.add(BeforeDestroyed.Literal.APPLICATION);
            beforeDestroyQualifiers.add(Any.Literal.INSTANCE);
            EventImpl.createNotifier(Object.class, Object.class, beforeDestroyQualifiers, this).notify(toString());
            // Destroy contexts
            applicationContext.destroy();
            // Fire an event with qualifier @Destroyed(ApplicationScoped.class)
            Set<Annotation> destroyQualifiers = new HashSet<>(4);
            destroyQualifiers.add(Destroyed.Literal.APPLICATION);
            destroyQualifiers.add(Any.Literal.INSTANCE);
            EventImpl.createNotifier(Object.class, Object.class, destroyQualifiers, this).notify(toString());
            singletonContext.destroy();
            requestContext.terminate();
            // Clear caches
            contexts.clear();
            beans.clear();
            resolved.clear();
            observers.clear();
            running.set(false);
            LOGGER.debugf("ArC DI container shut down");
        }
    }

    InstanceHandle<Object> getResource(Type type, Set<Annotation> annotations) {
        for (ResourceReferenceProvider resourceProvider : resourceProviders) {
            InstanceHandle<Object> ret = resourceProvider.get(type, annotations);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    private <T> InstanceHandle<T> instanceHandle(Type type, Annotation... qualifiers) {
        return beanInstanceHandle(getBean(type, qualifiers), null);
    }

    <T> InstanceHandle<T> beanInstanceHandle(InjectableBean<T> bean, CreationalContextImpl<T> parentContext) {
        if (bean != null) {
            if (parentContext == null && Dependent.class.equals(bean.getScope())) {
                parentContext = new CreationalContextImpl<>();
            }
            CreationalContextImpl<T> creationalContext = parentContext != null ? parentContext.child()
                    : new CreationalContextImpl<>();
            InjectionPoint prev = InjectionPointProvider.CURRENT.get();
            InjectionPointProvider.CURRENT.set(CurrentInjectionPointProvider.EMPTY);
            try {
                return new InstanceHandleImpl<T>(bean, bean.get(creationalContext), creationalContext, parentContext);
            } finally {
                if (prev != null) {
                    InjectionPointProvider.CURRENT.set(prev);
                } else {
                    InjectionPointProvider.CURRENT.remove();
                }
            }
        } else {
            return InstanceHandleImpl.unavailable();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> InjectableBean<T> getBean(Type requiredType, Annotation... qualifiers) {
        if (qualifiers == null || qualifiers.length == 0) {
            qualifiers = new Annotation[] { Default.Literal.INSTANCE };
        }
        Set<InjectableBean<?>> resolvedBeans = resolved.getValue(new Resolvable(requiredType, qualifiers));
        return resolvedBeans.isEmpty() || resolvedBeans.size() > 1 ? null : (InjectableBean<T>) resolvedBeans.iterator().next();
    }

    Set<Bean<?>> getBeans(Type requiredType, Annotation... qualifiers) {
        // This method does not cache the results
        return new HashSet<>(getMatchingBeans(new Resolvable(requiredType, qualifiers)));
    }

    Set<Bean<?>> getBeans(String name) {
        // This method does not cache the results
        return new HashSet<>(getMatchingBeans(name));
    }

    private Set<InjectableBean<?>> resolve(Resolvable resolvable) {
        return resolve(getMatchingBeans(resolvable));
    }

    private Set<InjectableBean<?>> resolve(String name) {
        return resolve(getMatchingBeans(name));
    }

    private InjectableBean<?> findById(String identifier) {
        for (InjectableBean<?> bean : beans) {
            if (bean.getIdentifier().equals(identifier)) {
                return bean;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static <X> Bean<? extends X> resolve(Set<Bean<? extends X>> beans) {
        if (beans == null || beans.isEmpty()) {
            return null;
        } else if (beans.size() == 1) {
            return beans.iterator().next();
        } else {
            // Try to resolve the ambiguity
            if (beans.stream().allMatch(b -> b instanceof InjectableBean)) {
                List<InjectableBean<?>> matching = new ArrayList<>();
                for (Bean<? extends X> bean : beans) {
                    matching.add((InjectableBean<? extends X>) bean);
                }
                Set<InjectableBean<?>> resolved = resolve(matching);
                if (resolved.size() != 1) {
                    throw new AmbiguousResolutionException(resolved.toString());
                }
                return (Bean<? extends X>) resolved.iterator().next();
            } else {
                // The set contains non-Arc beans - give our best effort
                Set<Bean<? extends X>> resolved = new HashSet<>(beans);
                for (Iterator<Bean<? extends X>> iterator = resolved.iterator(); iterator.hasNext();) {
                    if (!iterator.next().isAlternative()) {
                        iterator.remove();
                    }
                }
                if (resolved.size() != 1) {
                    throw new AmbiguousResolutionException(resolved.toString());
                }
                return resolved.iterator().next();
            }
        }
    }

    private static Set<InjectableBean<?>> resolve(List<InjectableBean<?>> matching) {
        if (matching.isEmpty()) {
            return Collections.emptySet();
        } else if (matching.size() == 1) {
            return Collections.singleton(matching.get(0));
        }
        // Try to resolve the ambiguity
        List<InjectableBean<?>> resolved = new ArrayList<>(matching);
        for (Iterator<InjectableBean<?>> iterator = resolved.iterator(); iterator.hasNext();) {
            InjectableBean<?> bean = iterator.next();
            if (bean.getAlternativePriority() == null
                    && (bean.getDeclaringBean() == null || bean.getDeclaringBean().getAlternativePriority() == null)) {
                // Remove non-alternatives
                iterator.remove();
            }
        }
        if (resolved.size() == 1) {
            return Collections.singleton(resolved.get(0));
        } else if (resolved.size() > 1) {
            resolved.sort(ArcContainerImpl::compareAlternativeBeans);
            // Keep only the highest priorities
            Integer highest = getAlternativePriority(resolved.get(0));
            for (Iterator<InjectableBean<?>> iterator = resolved.iterator(); iterator.hasNext();) {
                if (!highest.equals(getAlternativePriority(iterator.next()))) {
                    iterator.remove();
                }
            }
            if (resolved.size() == 1) {
                return Collections.singleton(resolved.get(0));
            }
        }
        return new HashSet<>(matching);
    }

    private static Integer getAlternativePriority(InjectableBean<?> bean) {
        return bean.getDeclaringBean() != null ? bean.getDeclaringBean().getAlternativePriority()
                : bean.getAlternativePriority();
    }

    List<InjectableBean<?>> getMatchingBeans(Resolvable resolvable) {
        List<InjectableBean<?>> matching = new ArrayList<>();
        for (InjectableBean<?> bean : beans) {
            if (matches(bean, resolvable.requiredType, resolvable.qualifiers)) {
                matching.add(bean);
            }
        }
        return matching;
    }

    List<InjectableBean<?>> getMatchingBeans(String name) {
        List<InjectableBean<?>> matching = new ArrayList<>();
        for (InjectableBean<?> bean : beans) {
            if (name.equals(bean.getName())) {
                matching.add(bean);
            }
        }
        return matching;
    }

    private static int compareAlternativeBeans(InjectableBean<?> bean1, InjectableBean<?> bean2) {
        // The highest priority wins
        Integer priority2 = bean2.getDeclaringBean() != null ? bean2.getDeclaringBean().getAlternativePriority()
                : bean2.getAlternativePriority();
        Integer priority1 = bean1.getDeclaringBean() != null ? bean1.getDeclaringBean().getAlternativePriority()
                : bean1.getAlternativePriority();
        return priority2.compareTo(priority1);
    }

    @SuppressWarnings("unchecked")
    <T> List<InjectableObserverMethod<? super T>> resolveObservers(Type eventType, Set<Annotation> eventQualifiers) {
        if (observers.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Type> eventTypes = new HierarchyDiscovery(eventType).getTypeClosure();
        List<InjectableObserverMethod<? super T>> resolvedObservers = new ArrayList<>();
        for (InjectableObserverMethod<?> observer : observers) {
            if (EventTypeAssignabilityRules.matches(observer.getObservedType(), eventTypes)) {
                if (observer.getObservedQualifiers().isEmpty()
                        || Qualifiers.isSubset(observer.getObservedQualifiers(), eventQualifiers)) {
                    resolvedObservers.add((InjectableObserverMethod<? super T>) observer);
                }
            }
        }
        // Observers with smaller priority values are called first
        Collections.sort(resolvedObservers, InjectableObserverMethod::compare);
        return resolvedObservers;
    }

    List<Interceptor<?>> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings) {
        if (interceptors.isEmpty()) {
            return Collections.emptyList();
        }
        List<Interceptor<?>> interceptors = new ArrayList<>();
        for (InjectableInterceptor<?> interceptor : this.interceptors) {
            if (interceptor.intercepts(type) && hasAllInterceptionBindings(interceptor, interceptorBindings)) {
                interceptors.add(interceptor);
            }
        }
        return interceptors;
    }

    private boolean hasAllInterceptionBindings(InjectableInterceptor<?> interceptor, Annotation[] interceptorBindings) {
        for (Annotation binding : interceptorBindings) {
            // The resolution rules are the same for qualifiers
            if (!Qualifiers.hasQualifier(interceptor.getInterceptorBindings(), binding)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Performs typesafe resolution and resolves ambiguities.
     *
     * @param requiredType
     * @param qualifiers
     * @return the set of resolved beans
     */
    Set<InjectableBean<?>> getResolvedBeans(Type requiredType, Annotation... qualifiers) {
        if (qualifiers == null || qualifiers.length == 0) {
            qualifiers = new Annotation[] { Default.Literal.INSTANCE };
        }
        return resolved.getValue(new Resolvable(requiredType, qualifiers));
    }

    private boolean matches(InjectableBean<?> bean, Type requiredType, Annotation... qualifiers) {
        if (!BeanTypeAssignabilityRules.matches(requiredType, bean.getTypes())) {
            return false;
        }
        return Qualifiers.hasQualifiers(bean, qualifiers);
    }

    static ArcContainerImpl unwrap(ArcContainer container) {
        if (container instanceof ArcContainerImpl) {
            return (ArcContainerImpl) container;
        } else {
            throw new IllegalArgumentException();
        }
    }

    static ArcContainerImpl instance() {
        return unwrap(Arc.container());
    }

    private void requireRunning() {
        if (!running.get()) {
            throw new IllegalStateException("Container not running: " + toString());
        }
    }

    private static final class Resolvable {

        final Type requiredType;

        final Annotation[] qualifiers;

        Resolvable(Type requiredType, Annotation[] qualifiers) {
            this.requiredType = requiredType;
            this.qualifiers = qualifiers;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(qualifiers);
            result = prime * result + ((requiredType == null) ? 0 : requiredType.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Resolvable)) {
                return false;
            }
            Resolvable other = (Resolvable) obj;
            if (requiredType == null) {
                if (other.requiredType != null) {
                    return false;
                }
            } else if (!requiredType.equals(other.requiredType)) {
                return false;
            }
            if (!Arrays.equals(qualifiers, other.qualifiers)) {
                return false;
            }
            return true;
        }

    }

}
