package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.InjectionPoint;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableReferenceProvider;

/**
 * Set the current {@link InjectionPoint} during creation of a dependent bean.
 */
public class CurrentInjectionPointProvider<T> implements InjectableReferenceProvider<T> {

    static final InjectionPoint EMPTY = new InjectionPointImpl(Object.class, Object.class, Collections.emptySet(), null, null,
            null, -1, false);

    static final Supplier<InjectionPoint> EMPTY_SUPPLIER = new Supplier<InjectionPoint>() {

        @Override
        public InjectionPoint get() {
            return CurrentInjectionPointProvider.EMPTY;
        }
    };

    private final Supplier<InjectableReferenceProvider<T>> delegateSupplier;

    private final InjectionPoint injectionPoint;

    public CurrentInjectionPointProvider(InjectableBean<?> bean, Supplier<InjectableReferenceProvider<T>> delegateSupplier,
            Type requiredType, Set<Annotation> qualifiers, Set<Annotation> annotations, Member javaMember, int position,
            boolean isTransient) {
        this.delegateSupplier = delegateSupplier;
        this.injectionPoint = new InjectionPointImpl(requiredType, requiredType, qualifiers, bean, annotations, javaMember,
                position, isTransient);
    }

    @Override
    public T get(CreationalContext<T> creationalContext) {
        InjectionPoint prev = InjectionPointProvider.setCurrent(creationalContext, injectionPoint);
        try {
            return delegateSupplier.get().get(creationalContext);
        } finally {
            InjectionPointProvider.setCurrent(creationalContext, prev);
        }
    }

    InjectableReferenceProvider<T> getDelegate() {
        return delegateSupplier.get();
    }

}
