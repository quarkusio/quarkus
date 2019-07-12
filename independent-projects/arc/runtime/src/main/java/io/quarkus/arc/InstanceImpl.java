package io.quarkus.arc;

import io.quarkus.arc.CurrentInjectionPointProvider.InjectionPointImpl;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Provider;

/**
 *
 * @author Martin Kouba
 */
class InstanceImpl<T> implements Instance<T> {

    private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[] {};

    private final CreationalContextImpl<?> creationalContext;
    private final Set<InjectableBean<?>> resolvedBeans;

    private final Type injectionPointType;
    private final Type requiredType;
    private final Set<Annotation> requiredQualifiers;
    private final InjectableBean<?> targetBean;
    private final Set<Annotation> annotations;
    private final Member javaMember;
    private final int position;

    InstanceImpl(InjectableBean<?> targetBean, Type type, Set<Annotation> qualifiers,
            CreationalContextImpl<?> creationalContext, Set<Annotation> annotations, Member javaMember, int position) {
        this(targetBean, type, getRequiredType(type), qualifiers, creationalContext, annotations, javaMember, position);
    }

    InstanceImpl(InstanceImpl<?> parent, Type requiredType, Set<Annotation> requiredQualifiers) {
        this(parent.targetBean, parent.injectionPointType, requiredType, requiredQualifiers, parent.creationalContext,
                parent.annotations, parent.javaMember, parent.position);
    }

    InstanceImpl(InjectableBean<?> targetBean, Type injectionPointType, Type requiredType, Set<Annotation> requiredQualifiers,
            CreationalContextImpl<?> creationalContext, Set<Annotation> annotations, Member javaMember, int position) {
        this.injectionPointType = injectionPointType;
        this.requiredType = requiredType;
        this.requiredQualifiers = requiredQualifiers != null ? requiredQualifiers : Collections.emptySet();
        this.creationalContext = creationalContext;
        if (this.requiredQualifiers.isEmpty() && Object.class.equals(requiredType)) {
            // Do not prefetch the beans for Instance<Object> with no qualifiers
            this.resolvedBeans = null;
        } else {
            this.resolvedBeans = resolve();
        }
        this.targetBean = targetBean;
        this.annotations = annotations;
        this.javaMember = javaMember;
        this.position = position;
    }

    @Override
    public Iterator<T> iterator() {
        return new InstanceIterator(beans());
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get() {
        Set<InjectableBean<?>> beans = beans();
        if (beans.isEmpty()) {
            throw new UnsatisfiedResolutionException();
        } else if (beans.size() > 1) {
            throw new AmbiguousResolutionException("Beans: " + beans.toString());
        }
        return getBeanInstance((InjectableBean<T>) beans.iterator().next());
    }

    @Override
    public Instance<T> select(Annotation... qualifiers) {
        Set<Annotation> newQualifiers = new HashSet<>(this.requiredQualifiers);
        Collections.addAll(newQualifiers, qualifiers);
        return new InstanceImpl<>(this, requiredType, newQualifiers);
    }

    @Override
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
        Set<Annotation> newQualifiers = new HashSet<>(this.requiredQualifiers);
        Collections.addAll(newQualifiers, qualifiers);
        return new InstanceImpl<>(this, subtype, newQualifiers);
    }

    @Override
    public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        Set<Annotation> newQualifiers = new HashSet<>(this.requiredQualifiers);
        Collections.addAll(newQualifiers, qualifiers);
        return new InstanceImpl<>(this, subtype.getType(), newQualifiers);
    }

    @Override
    public boolean isUnsatisfied() {
        return beans().isEmpty();
    }

    @Override
    public boolean isAmbiguous() {
        return beans().size() > 1;
    }

    @Override
    public void destroy(T instance) {
        Objects.requireNonNull(instance);
        if (instance instanceof ClientProxy) {
            ClientProxy proxy = (ClientProxy) instance;
            InjectableContext context = Arc.container().getActiveContext(proxy.arc_bean().getScope());
            if (context == null) {
                throw new ContextNotActiveException("No active context found for: " + proxy.arc_bean().getScope());
            }
            context.destroy(proxy.arc_bean());
        } else {
            // Try to destroy a dependent instance
            creationalContext.destroyDependentInstance(instance);
        }
    }

    void destroy() {
        creationalContext.release();
    }

    private T getBeanInstance(InjectableBean<T> bean) {
        CreationalContextImpl<T> ctx = creationalContext.child(bean);
        InjectionPoint prev = InjectionPointProvider
                .set(new InjectionPointImpl(injectionPointType, requiredType, requiredQualifiers, targetBean, annotations,
                        javaMember, position));
        T instance;
        try {
            instance = bean.get(ctx);
        } finally {
            InjectionPointProvider.set(prev);
        }
        return instance;
    }

    private Set<InjectableBean<?>> beans() {
        return resolvedBeans != null ? resolvedBeans : resolve();
    }

    private Set<InjectableBean<?>> resolve() {
        return ArcContainerImpl.instance().getResolvedBeans(requiredType, requiredQualifiers.toArray(EMPTY_ANNOTATION_ARRAY));
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

    private static Type getRequiredType(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (Provider.class.isAssignableFrom(Types.getRawType(parameterizedType.getRawType()))) {
                return parameterizedType.getActualTypeArguments()[0];
            }
        }
        throw new IllegalArgumentException("Not a valid type: " + type);
    }

}
