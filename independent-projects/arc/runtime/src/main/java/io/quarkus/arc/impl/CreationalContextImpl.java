package io.quarkus.arc.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.InjectionPoint;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableReferenceProvider;
import io.quarkus.arc.InstanceHandle;

/**
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
public class CreationalContextImpl<T> implements CreationalContext<T>, Function<Contextual<T>, CreationalContext<T>> {

    private final Contextual<T> contextual;
    private final CreationalContextImpl<?> parent;
    private List<InstanceHandle<?>> dependentInstances;

    private InjectionPoint currentInjectionPoint;
    private Object currentDecoratorDelegate;

    public CreationalContextImpl(Contextual<T> contextual) {
        this(contextual, null);
    }

    public CreationalContextImpl(Contextual<T> contextual, CreationalContextImpl<?> parent) {
        this.contextual = contextual;
        this.parent = parent;
        this.dependentInstances = null;
    }

    public <I> void addDependentInstance(InjectableBean<I> bean, I instance, CreationalContext<I> ctx) {
        addDependentInstance(new EagerInstanceHandle<I>(bean, instance, ctx));
    }

    public synchronized <I> void addDependentInstance(InstanceHandle<I> instanceHandle) {
        if (dependentInstances == null) {
            dependentInstances = new ArrayList<>();
        }
        dependentInstances.add(instanceHandle);
    }

    public synchronized boolean hasDependentInstances() {
        return dependentInstances != null && !dependentInstances.isEmpty();
    }

    public synchronized boolean removeDependentInstance(Object dependentInstance, boolean destroy) {
        if (dependentInstances != null) {
            for (Iterator<InstanceHandle<?>> it = dependentInstances.iterator(); it.hasNext();) {
                InstanceHandle<?> handle = it.next();
                // The reference equality is used on purpose!
                if (handle.get() == dependentInstance) {
                    if (destroy) {
                        handle.destroy();
                    }
                    it.remove();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void push(T incompleteInstance) {
        // No-op
    }

    @Override
    public void release() {
        synchronized (this) {
            if (dependentInstances != null) {
                for (InstanceHandle<?> instance : dependentInstances) {
                    instance.destroy();
                }
            }
        }
    }

    public CreationalContextImpl<?> getParent() {
        return parent;
    }

    /**
     * @return the contextual or {@code null}
     */
    public Contextual<T> getContextual() {
        return contextual;
    }

    public <C> CreationalContextImpl<C> child(Contextual<C> contextual) {
        return new CreationalContextImpl<>(contextual, this);
    }

    @Override
    public CreationalContext<T> apply(Contextual<T> contextual) {
        return this;
    }

    public static <T> CreationalContextImpl<T> unwrap(CreationalContext<T> ctx) {
        if (ctx instanceof CreationalContextImpl) {
            return (CreationalContextImpl<T>) ctx;
        } else if (ctx instanceof SyntheticCreationalContextImpl) {
            return unwrap(((SyntheticCreationalContextImpl<T>) ctx).creationalContext);
        } else {
            throw new IllegalArgumentException("Failed to unwrap CreationalContextImpl: " + ctx);
        }
    }

    public static <C> CreationalContextImpl<C> child(CreationalContext<?> creationalContext) {
        return child(null, creationalContext);
    }

    @SuppressWarnings("unchecked")
    public static <C> CreationalContextImpl<C> child(InjectableReferenceProvider<?> provider,
            CreationalContext<?> creationalContext) {
        return unwrap(creationalContext).child(provider instanceof InjectableBean ? (InjectableBean<C>) provider : null);
    }

    public static <I> void addDependencyToParent(InjectableBean<I> bean, I instance, CreationalContext<I> ctx) {
        CreationalContextImpl<?> parent = unwrap(ctx).getParent();
        if (parent != null) {
            parent.addDependentInstance(bean, instance, ctx);
        }
    }

    static <T> InjectionPoint getCurrentInjectionPoint(CreationalContext<T> ctx) {
        CreationalContextImpl<?> instance = unwrap(ctx);
        while (instance != null) {
            synchronized (instance) {
                InjectionPoint result = instance.currentInjectionPoint;
                if (result != null) {
                    return result;
                }
            }
            instance = instance.parent;
        }
        return null;
    }

    static <T> InjectionPoint setCurrentInjectionPoint(CreationalContext<T> ctx, InjectionPoint injectionPoint) {
        CreationalContextImpl<T> instance = unwrap(ctx);
        synchronized (instance) {
            InjectionPoint previous = instance.currentInjectionPoint;
            instance.currentInjectionPoint = injectionPoint;
            return previous;
        }
    }

    static <T> Object getCurrentDecoratorDelegate(CreationalContext<T> ctx) {
        CreationalContextImpl<?> instance = unwrap(ctx);
        while (instance != null) {
            synchronized (instance) {
                Object result = instance.currentDecoratorDelegate;
                if (result != null) {
                    return result;
                }
            }
            instance = instance.parent;
        }
        return null;
    }

    static <T> Object setCurrentDecoratorDelegate(CreationalContext<T> ctx, Object decoratorDelegate) {
        CreationalContextImpl<T> instance = unwrap(ctx);
        synchronized (instance) {
            Object previous = instance.currentDecoratorDelegate;
            instance.currentDecoratorDelegate = decoratorDelegate;
            return previous;
        }
    }

}
