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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import javax.enterprise.inject.spi.InterceptionFactory;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProducerFactory;
import javax.enterprise.util.TypeLiteral;
import javax.interceptor.InterceptorBinding;

/**
 * @author Martin Kouba
 */
public class BeanManagerImpl implements BeanManager {

    @SuppressWarnings("serial")
    static final TypeLiteral<Instance<Object>> INSTANCE_LITERAL = new TypeLiteral<Instance<Object>>() {
    };

    static final LazyValue<BeanManagerImpl> INSTANCE = new LazyValue<>(BeanManagerImpl::new);

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Object getReference(Bean<?> bean, Type beanType, CreationalContext<?> ctx) {
        Objects.requireNonNull(bean, "Bean is null");
        Objects.requireNonNull(beanType, "Bean type is null");
        Objects.requireNonNull(ctx, "CreationalContext is null");
        if (bean instanceof InjectableBean && ctx instanceof CreationalContextImpl) {
            return ArcContainerImpl.instance().beanInstanceHandle((InjectableBean) bean, (CreationalContextImpl) ctx).get();
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
                return ArcContainerImpl.instance().beanInstanceHandle(bean, (CreationalContextImpl) ctx, false).get();
            } finally {
                InjectionPointProvider.set(prev);
            }
        }
        throw new IllegalArgumentException(
                "CreationalContext must be an instances of " + CreationalContextImpl.class);
    }

    @Override
    public <T> CreationalContext<T> createCreationalContext(Contextual<T> contextual) {
        return new CreationalContextImpl<>();
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
    public void fireEvent(Object event, Annotation... qualifiers) {
        getEvent().select(qualifiers).fire(event);
    }

    @Override
    public <T> Set<ObserverMethod<? super T>> resolveObserverMethods(T event, Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Decorator<?>> resolveDecorators(Set<Type> types, Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Interceptor<?>> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings) {
        return ArcContainerImpl.instance().resolveInterceptors(Objects.requireNonNull(type), interceptorBindings);
    }

    @Override
    public boolean isScope(Class<? extends Annotation> annotationType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isNormalScope(Class<? extends Annotation> annotationType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPassivatingScope(Class<? extends Annotation> annotationType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isQualifier(Class<? extends Annotation> annotationType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInterceptorBinding(Class<? extends Annotation> annotationType) {
        return annotationType.isAnnotationPresent(InterceptorBinding.class);
    }

    @Override
    public boolean isStereotype(Class<? extends Annotation> annotationType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Annotation> getInterceptorBindingDefinition(Class<? extends Annotation> bindingType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Annotation> getStereotypeDefinition(Class<? extends Annotation> stereotype) {
        throw new UnsupportedOperationException();
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
    public <T> InjectionTarget<T> createInjectionTarget(AnnotatedType<T> type) {
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
        return new EventImpl<>(Object.class, new HashSet<>());
    }

    @Override
    public Instance<Object> createInstance() {
        return new InstanceImpl<>(null, INSTANCE_LITERAL.getType(), Collections.emptySet(), new CreationalContextImpl<>(),
                Collections.emptySet(), null, -1);
    }

}
