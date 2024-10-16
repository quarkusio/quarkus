package io.quarkus.hibernate.search.standalone.elasticsearch.runtime.bean;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;

final class ArcBeanReference<T> implements BeanReference<T> {

    private final InjectableBean<T> bean;

    public ArcBeanReference(InjectableBean<T> bean) {
        this.bean = bean;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + bean.toString() + "]";
    }

    @Override
    public BeanHolder<T> resolve(BeanResolver beanResolver) {
        return new ArcBeanHolder<>(Arc.container().instance(bean));
    }

}
