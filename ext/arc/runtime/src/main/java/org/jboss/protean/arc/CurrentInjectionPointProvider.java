package org.jboss.protean.arc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 *
 * @author Martin Kouba
 */
public class CurrentInjectionPointProvider<T> implements InjectableReferenceProvider<T> {

    static final InjectionPoint EMPTY = new InjectionPointImpl(Object.class, Collections.emptySet());

    private final InjectableReferenceProvider<T> delegate;

    private final InjectionPoint injectionPoint;

    public CurrentInjectionPointProvider(InjectableReferenceProvider<T> delegate, Type requiredType, Set<Annotation> qualifiers) {
        this.delegate = delegate;
        this.injectionPoint = new InjectionPointImpl(requiredType, qualifiers);
    }

    @Override
    public T get(CreationalContext<T> creationalContext) {
        InjectionPoint prev = InjectionPointProvider.CURRENT.get();
        InjectionPointProvider.CURRENT.set(injectionPoint);
        try {
            return delegate.get(creationalContext);
        } finally {
            if (prev != null) {
                InjectionPointProvider.CURRENT.set(prev);
            } else {
                InjectionPointProvider.CURRENT.remove();
            }
        }
    }

    private static class InjectionPointImpl implements InjectionPoint {

        private final Type requiredType;

        private final Set<Annotation> qualifiers;

        InjectionPointImpl(Type requiredType, Set<Annotation> qualifiers) {
            this.requiredType = requiredType;
            this.qualifiers = qualifiers;
        }

        @Override
        public Type getType() {
            return requiredType;
        }

        @Override
        public Set<Annotation> getQualifiers() {
            return qualifiers;
        }

        @Override
        public Bean<?> getBean() {
            return null;
        }

        @Override
        public Member getMember() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Annotated getAnnotated() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isDelegate() {
            return false;
        }

        @Override
        public boolean isTransient() {
            return false;
        }

    }

}
