package io.quarkus.hibernate.search.standalone.elasticsearch.runtime.bean;

import org.hibernate.search.engine.environment.bean.BeanHolder;

import io.quarkus.arc.InstanceHandle;

final class ArcBeanHolder<T> implements BeanHolder<T> {
    private final InstanceHandle<T> handle;

    public ArcBeanHolder(InstanceHandle<T> handle) {
        this.handle = handle;
    }

    @Override
    public T get() {
        return handle.get();
    }

    @Override
    public void close() {
        handle.destroy();
    }
}
