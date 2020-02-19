package io.quarkus.hibernate.orm.runtime.boot.fakebeanmanager;

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

public class FakeInjectionTarget<T> implements InjectionTarget<T> {
    private final AnnotatedType<T> type;
    private CDI<Object> cdi;

    public FakeInjectionTarget(AnnotatedType<T> type, CDI<Object> cdi) {
        this.type = type;
        this.cdi = cdi;
    }

    @Override
    public void inject(T instance, CreationalContext<T> ctx) {

    }

    @Override
    public void postConstruct(T instance) {

    }

    @Override
    public void preDestroy(T instance) {

    }

    @Override
    public T produce(CreationalContext<T> ctx) {
        return cdi.select(type.getJavaClass()).get();
    }

    @Override
    public void dispose(T instance) {
        cdi.destroy(instance);

    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return null;
    }
}
