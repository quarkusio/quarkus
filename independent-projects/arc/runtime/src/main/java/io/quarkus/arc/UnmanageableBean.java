package io.quarkus.arc;

import javax.enterprise.context.spi.CreationalContext;

public interface UnmanageableBean<T> extends InjectableBean<T> {

    T produce(CreationalContext<T> ctx);

    void inject(T instance, CreationalContext<T> ctx);

    void postConstruct(T instance);

    void preDestroy(T instance);

    void dispose(T instance);
}
