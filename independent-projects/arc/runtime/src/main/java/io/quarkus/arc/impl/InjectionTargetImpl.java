package io.quarkus.arc.impl;

import java.util.Set;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

public class InjectionTargetImpl<T> implements InjectionTarget<T> {
    private Bean<T> bean;
    private BeanManager beanManager;
    private AnnotatedType<T> annotatedType;

    public InjectionTargetImpl(Bean<T> bean, BeanManager beanManager, AnnotatedType<T> annotatedType) {
        this.bean = bean;
        this.beanManager = beanManager;
        this.annotatedType = annotatedType;
    }

    @Override
    public void inject(T instance, CreationalContext<T> ctx) {
/*Performs dependency injection upon the given object. Performs Jakarta EE component environment injection, sets the value of all injected fields, and calls all initializer methods.
Params:
instance – The instance upon which to perform injection
ctx – The CreationalContext to use for creating new instances*/
    }

    @Override
    public void postConstruct(T instance) {
        /*Calls the javax.annotation.PostConstruct callback, if it exists, according to the semantics required by the Java EE platform specification.
Params:
instance – The instance on which to invoke the javax.annotation.PostConstruct method*/
        //instance.getClass().getMethods();
    }

    @Override
    public void preDestroy(T instance) {
        /*Calls the javax.annotation.PreDestroy callback, if it exists, according to the semantics required by the Jakarta EE platform specification.
Params:
instance – The instance on which to invoke the javax.annotation.PreDestroy method*/
        //instance.getClass().getMethods();
        // bean contain
    }

    @Override
    public T produce(CreationalContext<T> ctx) {
        //here is the issue
        // bean is empty because UnManaged call with null param.
        // so we have to create it on demand
        if (bean == null) {
            bean = beanManager.createBean(,annotatedType.getJavaClass());
        }
        return bean.create(ctx);
    }

    @Override
    public void dispose(T instance) {

    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return null;
    }
}
