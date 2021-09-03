package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.UnmanageableBean;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Qualifier;

public class InjectionTargetImpl<T> implements InjectionTarget<T> {
    private Bean<T> bean;
    private BeanManager beanManager;
    private AnnotatedType<T> annotatedType;
    private InjectionTargetFactoryImpl injectionTargetFactoryImpl;

    @SuppressWarnings("unchecked")
    public InjectionTargetImpl(Bean<T> bean, BeanManager beanManager, AnnotatedType<T> annotatedType,
            InjectionTargetFactoryImpl injectionTargetFactoryImpl) {
        this.bean = bean;
        this.beanManager = beanManager;
        this.annotatedType = annotatedType;
        this.injectionTargetFactoryImpl = injectionTargetFactoryImpl;
        if (bean == null) {
            Set<Annotation> qualifiers = new HashSet<>();
            for (Annotation a : annotatedType.getAnnotations()) {
                if (a.annotationType().isAnnotationPresent(Qualifier.class)) {
                    qualifiers.add(a);
                }
            }
            Type requiredType = annotatedType.getBaseType();
            Set<InjectableBean<?>> beans = ArcContainerImpl.instance()
                    .getResolvedBeans(requiredType, qualifiers.toArray(new Annotation[] {}));
            if (beans.isEmpty()) {
                throw new UnsatisfiedResolutionException(
                        "No bean found for required type [" + requiredType + "] and qualifiers [" + qualifiers + "]");
            } else if (beans.size() > 1) {
                throw new AmbiguousResolutionException("Beans: " + beans.toString());
            }
            this.bean = (InjectableBean<T>) beans.iterator().next();
        }
    }

    @Override
    public void inject(T instance, CreationalContext<T> ctx) {
        if (bean instanceof UnmanageableBean) {
            ((UnmanageableBean<T>) bean).inject(instance, ctx);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void postConstruct(T instance) {
        if (bean instanceof UnmanageableBean) {
            ((UnmanageableBean<T>) bean).postConstruct(instance);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void preDestroy(T instance) {
        if (bean instanceof UnmanageableBean) {
            ((UnmanageableBean<T>) bean).preDestroy(instance);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public T produce(CreationalContext<T> ctx) {
        if (bean instanceof UnmanageableBean) {
            return ((UnmanageableBean<T>) bean).produce(ctx);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void dispose(T instance) {
        if (bean instanceof UnmanageableBean) {
            ((UnmanageableBean<T>) bean).dispose(instance);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return bean.getInjectionPoints();
    }
}
