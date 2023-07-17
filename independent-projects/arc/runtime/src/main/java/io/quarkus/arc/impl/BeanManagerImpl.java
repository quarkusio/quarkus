package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMember;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Decorator;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTargetFactory;
import jakarta.enterprise.inject.spi.InterceptionFactory;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.enterprise.inject.spi.ProducerFactory;
import jakarta.inject.Qualifier;
import jakarta.interceptor.InterceptorBinding;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;

/**
 * @author Martin Kouba
 */
public class BeanManagerImpl implements BeanManager {

    static final LazyValue<BeanManagerImpl> INSTANCE = new LazyValue<>(BeanManagerImpl::new);

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Object getReference(Bean<?> bean, Type beanType, CreationalContext<?> ctx) {
        Objects.requireNonNull(bean, "Bean is null");
        Objects.requireNonNull(beanType, "Bean type is null");
        Objects.requireNonNull(ctx, "CreationalContext is null");
        if (!BeanTypeAssignabilityRules.instance().matches(beanType, bean.getTypes())) {
            throw new IllegalArgumentException("Type " + beanType + " is not a bean type of " + bean
                    + "; its bean types are: " + bean.getTypes());
        }
        if (bean instanceof InjectableBean && ctx instanceof CreationalContextImpl) {
            // there's no actual injection point or an `Instance` object,
            // the "current" injection point must be `null`
            InjectionPoint prev = InjectionPointProvider.set(null);
            try {
                return ArcContainerImpl.beanInstanceHandle((InjectableBean) bean, (CreationalContextImpl) ctx,
                        false, null, true).get();
            } finally {
                InjectionPointProvider.set(prev);
            }
        }
        throw new IllegalArgumentException(
                "Arguments must be instances of " + InjectableBean.class + " and " + CreationalContextImpl.class + ": \nbean: "
                        + bean + "\nctx: " + ctx);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Object getInjectableReference(InjectionPoint ij, CreationalContext<?> ctx) {
        Objects.requireNonNull(ij, "InjectionPoint is null");
        Objects.requireNonNull(ctx, "CreationalContext is null");
        if (ctx instanceof CreationalContextImpl) {
            Set<Bean<?>> beans = getBeans(ij.getType(), ij.getQualifiers().toArray(new Annotation[] {}));
            if (beans.isEmpty()) {
                throw new UnsatisfiedResolutionException();
            }
            InjectableBean<?> bean = (InjectableBean<?>) resolve(beans);
            InjectionPoint prev = InjectionPointProvider.set(ij);
            try {
                return ArcContainerImpl.beanInstanceHandle(bean, (CreationalContextImpl) ctx,
                        false, null, true).get();
            } finally {
                InjectionPointProvider.set(prev);
            }
        }
        throw new IllegalArgumentException(
                "CreationalContext must be an instances of " + CreationalContextImpl.class);
    }

    @Override
    public <T> CreationalContext<T> createCreationalContext(Contextual<T> contextual) {
        return new CreationalContextImpl<>(contextual);
    }

    @Override
    public Set<Bean<?>> getBeans(Type beanType, Annotation... qualifiers) {
        return ArcContainerImpl.instance().getBeans(Objects.requireNonNull(beanType), qualifiers);
    }

    @Override
    public Set<Bean<?>> getBeans(String name) {
        return ArcContainerImpl.instance().getBeans(Objects.requireNonNull(name));
    }

    @Override
    public Bean<?> getPassivationCapableBean(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <X> Bean<? extends X> resolve(Set<Bean<? extends X>> beans) {
        return ArcContainerImpl.resolve(beans);
    }

    @Override
    public void validate(InjectionPoint injectionPoint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Set<ObserverMethod<? super T>> resolveObserverMethods(T event, Annotation... qualifiers) {
        Type eventType = Types.getCanonicalType(event.getClass());
        if (Types.containsTypeVariable(eventType)) {
            throw new IllegalArgumentException("The runtime type of the event object contains a type variable: " + eventType);
        }
        Set<Annotation> eventQualifiers = new HashSet<>(Arrays.asList(qualifiers));
        return new LinkedHashSet<>(ArcContainerImpl.instance().resolveObservers(eventType, eventQualifiers));
    }

    @Override
    public List<Decorator<?>> resolveDecorators(Set<Type> types, Annotation... qualifiers) {
        return ArcContainerImpl.instance().resolveDecorators(types, qualifiers);
    }

    @Override
    public List<Interceptor<?>> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings) {
        return ArcContainerImpl.instance().resolveInterceptors(Objects.requireNonNull(type), interceptorBindings);
    }

    @Override
    public boolean isScope(Class<? extends Annotation> annotationType) {
        return ArcContainerImpl.instance().isScope(annotationType);
    }

    @Override
    public boolean isNormalScope(Class<? extends Annotation> annotationType) {
        // Note that it's possible to register a custom context with a scope annotation that is not annotated with @Scope or @NormalScope
        return ArcContainerImpl.instance().isNormalScope(annotationType);
    }

    @Override
    public boolean isPassivatingScope(Class<? extends Annotation> annotationType) {
        // return false instead of a UnsupportedOperationException, so libs like DeltaSpike can handle it "nicer"
        return false;
    }

    @Override
    public boolean isQualifier(Class<? extends Annotation> annotationType) {
        return annotationType.isAnnotationPresent(Qualifier.class)
                || ArcContainerImpl.instance().registeredQualifiers.isRegistered(annotationType);
    }

    @Override
    public boolean isInterceptorBinding(Class<? extends Annotation> annotationType) {
        return annotationType.isAnnotationPresent(InterceptorBinding.class)
                || ArcContainerImpl.instance().registeredInterceptorBindings.isRegistered(annotationType);
    }

    @Override
    public boolean isStereotype(Class<? extends Annotation> annotationType) {
        //best effort, in theory an extension could change this but this is an edge case
        return annotationType.isAnnotationPresent(Stereotype.class);
    }

    @Override
    public Set<Annotation> getInterceptorBindingDefinition(Class<? extends Annotation> bindingType) {
        //best effort, in theory an extension could change this its better than nothing
        //it could also return annotations that aren't bindings
        return new HashSet<>(Arrays.asList(bindingType.getAnnotations()));
    }

    @Override
    public Set<Annotation> getStereotypeDefinition(Class<? extends Annotation> stereotype) {
        //best effort, in theory an extension could change this its better than nothing
        //it could also return annotations that aren't metadata
        return new HashSet<>(Arrays.asList(stereotype.getAnnotations()));
    }

    @Override
    public boolean areQualifiersEquivalent(Annotation qualifier1, Annotation qualifier2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean areInterceptorBindingsEquivalent(Annotation interceptorBinding1, Annotation interceptorBinding2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getQualifierHashCode(Annotation qualifier) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getInterceptorBindingHashCode(Annotation interceptorBinding) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Context getContext(Class<? extends Annotation> scopeType) {
        Context context = Arc.container().getActiveContext(scopeType);
        if (context == null) {
            throw new ContextNotActiveException("No active context found for: " + scopeType);
        }
        return context;
    }

    @Override
    public ELResolver getELResolver() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ExpressionFactory wrapExpressionFactory(ExpressionFactory expressionFactory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> AnnotatedType<T> createAnnotatedType(Class<T> type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> InjectionTargetFactory<T> getInjectionTargetFactory(AnnotatedType<T> annotatedType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <X> ProducerFactory<X> getProducerFactory(AnnotatedField<? super X> field, Bean<X> declaringBean) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <X> ProducerFactory<X> getProducerFactory(AnnotatedMethod<? super X> method, Bean<X> declaringBean) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> BeanAttributes<T> createBeanAttributes(AnnotatedType<T> type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BeanAttributes<?> createBeanAttributes(AnnotatedMember<?> type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Bean<T> createBean(BeanAttributes<T> attributes, Class<T> beanClass,
            InjectionTargetFactory<T> injectionTargetFactory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T, X> Bean<T> createBean(BeanAttributes<T> attributes, Class<X> beanClass, ProducerFactory<X> producerFactory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InjectionPoint createInjectionPoint(AnnotatedField<?> field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InjectionPoint createInjectionPoint(AnnotatedParameter<?> parameter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Extension> T getExtension(Class<T> extensionClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> InterceptionFactory<T> createInterceptionFactory(CreationalContext<T> ctx, Class<T> clazz) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Event<Object> getEvent() {
        return new EventImpl<>(Object.class, new HashSet<>(), null);
    }

    @Override
    public Instance<Object> createInstance() {
        return ArcContainerImpl.instance().instance;
    }

}
