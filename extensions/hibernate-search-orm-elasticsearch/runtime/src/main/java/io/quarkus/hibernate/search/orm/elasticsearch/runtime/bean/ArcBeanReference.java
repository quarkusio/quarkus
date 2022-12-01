package io.quarkus.hibernate.search.orm.elasticsearch.runtime.bean;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;

import io.quarkus.arc.InjectableInstance;

final class ArcBeanReference<T> implements BeanReference<T> {

    private final InjectableInstance<T> delegate;

    public ArcBeanReference(InjectableInstance<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + delegate.toString() + "]";
    }

    @Override
    public BeanHolder<T> resolve(BeanResolver beanResolver) {
        return new ArcBeanHolder<>(delegate.getHandle());
    }

}
