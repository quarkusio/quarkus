package org.jboss.protean.arc;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.util.TypeLiteral;

/**
 *
 * @author Martin Kouba
 */
class InstanceImpl<T> implements Instance<T> {

    private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[] {};

    private final Type type;

    private final Set<Annotation> qualifiers;

    private final CreationalContextImpl<?> creationalContext;

    InstanceImpl(Type type, Set<Annotation> qualifiers, CreationalContextImpl<?> creationalContext) {
        if (type instanceof ParameterizedType) {
            this.type = ((ParameterizedType) type).getActualTypeArguments()[0];
        } else {
            throw new IllegalArgumentException();
        }
        this.qualifiers = qualifiers != null ? qualifiers : Collections.emptySet();
        this.creationalContext = creationalContext;
    }

    @Override
    public Iterator<T> iterator() {
        return new InstanceIterator(getBeans());
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get() {
        List<InjectableBean<?>> beans = getBeans();
        if (beans.isEmpty()) {
            throw new UnsatisfiedResolutionException();
        } else if (beans.size() > 1) {
            throw new AmbiguousResolutionException();
        }
        return getBeanInstance((InjectableBean<T>) beans.get(0));
    }

    @Override
    public Instance<T> select(Annotation... qualifiers) {
        Set<Annotation> newQualifiers = new HashSet<>(this.qualifiers);
        Collections.addAll(newQualifiers, qualifiers);
        return new InstanceImpl<>(type, newQualifiers, creationalContext);
    }

    @Override
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
        Set<Annotation> newQualifiers = new HashSet<>(this.qualifiers);
        Collections.addAll(newQualifiers, qualifiers);
        return new InstanceImpl<>(type, newQualifiers, creationalContext);
    }

    @Override
    public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        Set<Annotation> newQualifiers = new HashSet<>(this.qualifiers);
        Collections.addAll(newQualifiers, qualifiers);
        return new InstanceImpl<>(subtype.getType(), newQualifiers, creationalContext);
    }

    @Override
    public boolean isUnsatisfied() {
        return getBeans().isEmpty();
    }

    @Override
    public boolean isAmbiguous() {
        return getBeans().size() > 1;
    }

    @Override
    public void destroy(T instance) {
        if (instance instanceof ClientProxy) {
            throw new UnsupportedOperationException();
        }
        // Try to destroy a dependent instance
        creationalContext.destroyDependentInstance(instance);
    }

    private T getBeanInstance(InjectableBean<T> bean) {
        CreationalContextImpl<T> ctx = creationalContext.child();
        // TODO current injection point?
        T instance = bean.get(ctx);
        if (Dependent.class.equals(bean.getScope())) {
            creationalContext.addDependentInstance(bean, instance, ctx);
        }
        return instance;
    }

    private List<InjectableBean<?>> getBeans() {
        return ArcContainerImpl.unwrap(Arc.container()).geBeans(type, qualifiers.toArray(EMPTY_ANNOTATION_ARRAY));
    }

    class InstanceIterator implements Iterator<T> {

        protected final Iterator<InjectableBean<?>> delegate;

        private InstanceIterator(Collection<InjectableBean<?>> beans) {
            this.delegate = beans.iterator();
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("unchecked")
        @Override
        public T next() {
            return getBeanInstance((InjectableBean<T>) delegate.next());
        }

    }

}
