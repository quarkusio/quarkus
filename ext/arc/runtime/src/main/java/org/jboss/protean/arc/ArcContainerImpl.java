package org.jboss.protean.arc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Singleton;

/**
 *
 * @author Martin Kouba
 */
class ArcContainerImpl implements ArcContainer {

    private final String id;

    private final AtomicBoolean running;

    private final List<InjectableBean<?>> beans;

    private final List<InjectableObserverMethod<?>> observers;

    private final Map<Class<? extends Annotation>, InjectableContext> contexts;

    private final ComputingCache<Resolvable, List<InjectableBean<?>>> resolved;

    public ArcContainerImpl() {
        id = UUID.randomUUID().toString();
        running = new AtomicBoolean(true);
        beans = new CopyOnWriteArrayList<>();
        observers = new CopyOnWriteArrayList<>();
        for (ComponentsProvider componentsProvider : ServiceLoader.load(ComponentsProvider.class)) {
            Components components = componentsProvider.getComponents();
            beans.addAll(components.getBeans());
            observers.addAll(components.getObservers());
        }
        contexts = new HashMap<>();
        contexts.put(ApplicationScoped.class, new ApplicationContext());
        contexts.put(Singleton.class, new SingletonContext());
        contexts.put(RequestScoped.class, new RequestContext());
        resolved = new ComputingCache<>(this::resolve);
    }

    void init() {
        requireRunning();
        // Fire an event with qualifier @Initialized(ApplicationScoped.class)
        Set<Annotation> qualifiers = new HashSet<>(4);
        qualifiers.add(Initialized.Literal.APPLICATION);
        qualifiers.add(Any.Literal.INSTANCE);
        EventImpl.createNotifier(Object.class, qualifiers, this).notify(toString());
    }

    @Override
    public InjectableContext getContext(Class<? extends Annotation> scopeType) {
        requireRunning();
        return contexts.get(scopeType);
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
    public ManagedContext requestContext() {
        requireRunning();
        return (ManagedContext) getContext(RequestScoped.class);
    }

    @Override
    public Runnable withinRequest(Runnable action) {
        return () -> {
            requireRunning();
            ManagedContext requestContext = requestContext();
            if (requestContext.isActive()) {
                action.run();
            } else {
                try {
                    requestContext.activate();
                    action.run();
                } finally {
                    requestContext.terminate();
                }
            }
        };
    }

    @Override
    public <T> Supplier<T> withinRequest(Supplier<T> action) {
        return () -> {
            requireRunning();
            ManagedContext requestContext = requestContext();
            if (requestContext.isActive()) {
                return action.get();
            } else {
                try {
                    requestContext.activate();
                    return action.get();
                } finally {
                    requestContext.terminate();
                }
            }
        };
    }

    @Override
    public String toString() {
        return "ArcContainerImpl [id=" + id + ", running=" + running + ", beans=" + beans.size() + ", observers=" + observers.size() + ", contexts="
                + contexts.size() + "]";
    }

    void shutdown() {
        if (running.compareAndSet(true, false)) {
            synchronized (this) {
                contexts.get(ApplicationScoped.class).destroy();
                contexts.get(Singleton.class).destroy();
                ((RequestContext) contexts.get(RequestScoped.class)).terminate();
                contexts.clear();
                beans.clear();
                resolved.clear();
                observers.clear();
            }
        }
    }

    private <T> InstanceHandle<T> instanceHandle(Type type, Annotation... qualifiers) {
        return instance(getBean(type, qualifiers));
    }

    private <T> InstanceHandle<T> instance(InjectableBean<T> bean) {
        if (bean != null) {
            CreationalContextImpl<T> parentContext = null;
            if (Dependent.class.equals(bean.getScope())) {
                parentContext = new CreationalContextImpl<>();
            }
            CreationalContextImpl<T> creationalContext = parentContext != null ? parentContext.child() : new CreationalContextImpl<>();
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
        List<InjectableBean<?>> resolvedBeans = resolved.getValue(new Resolvable(requiredType, qualifiers));
        return resolvedBeans.isEmpty() ? null : (InjectableBean<T>) resolvedBeans.get(0);
    }

    private List<InjectableBean<?>> resolve(Resolvable resolvable) {
        List<InjectableBean<?>> resolvedBeans = new ArrayList<>();
        for (InjectableBean<?> bean : beans) {
            if (matches(bean, resolvable.requiredType, resolvable.qualifiers)) {
                resolvedBeans.add(bean);
            }
        }
        if (resolvedBeans.size() > 1) {
            // Try to resolve the ambiguity
            for (Iterator<InjectableBean<?>> iterator = resolvedBeans.iterator(); iterator.hasNext();) {
                InjectableBean<?> bean = iterator.next();
                if (bean.getAlternativePriority() == null && (bean.getDeclaringBean() == null || bean.getDeclaringBean().getAlternativePriority() == null)) {
                    iterator.remove();
                }
            }
            if (resolvedBeans.size() > 1) {
                resolvedBeans.sort(this::compareAlternativeBeans);
            }
        }
        return resolvedBeans;
    }

    private int compareAlternativeBeans(InjectableBean<?> bean1, InjectableBean<?> bean2) {
        // The highest priority wins
        Integer priority2 = bean2.getDeclaringBean() != null ? bean2.getDeclaringBean().getAlternativePriority() : bean2.getAlternativePriority();
        Integer priority1 = bean1.getDeclaringBean() != null ? bean1.getDeclaringBean().getAlternativePriority() : bean1.getAlternativePriority();
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
                if (observer.getObservedQualifiers().isEmpty() || Qualifiers.isSubset(observer.getObservedQualifiers(), eventQualifiers)) {
                    resolvedObservers.add((InjectableObserverMethod<? super T>) observer);
                }
            }
        }
        // Observers with smaller priority values are called first
        Collections.sort(resolvedObservers, InjectableObserverMethod::compare);
        return resolvedObservers;
    }

    List<InjectableBean<?>> geBeans(Type requiredType, Annotation... qualifiers) {
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

    static <T> ArcContainerImpl unwrap(ArcContainer container) {
        if (container instanceof ArcContainerImpl) {
            return (ArcContainerImpl) container;
        } else {
            throw new IllegalArgumentException();
        }
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
