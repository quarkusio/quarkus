package io.quarkus.arc;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;

/**
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
class InstanceHandleImpl<T> implements InstanceHandle<T> {

    @SuppressWarnings("unchecked")
    public static final <T> InstanceHandle<T> unavailable() {
        return (InstanceHandle<T>) UNAVAILABLE;
    }

    static final InstanceHandleImpl<Object> UNAVAILABLE = new InstanceHandleImpl<Object>(null, null, null, null);

    private final InjectableBean<T> bean;

    private final T instance;

    private final CreationalContext<T> creationalContext;

    private final CreationalContext<?> parentCreationalContext;

    private final AtomicBoolean destroyed;

    InstanceHandleImpl(InjectableBean<T> bean, T instance, CreationalContext<T> creationalContext) {
        this(bean, instance, creationalContext, null);
    }

    InstanceHandleImpl(InjectableBean<T> bean, T instance, CreationalContext<T> creationalContext,
            CreationalContext<?> parentCreationalContext) {
        this.bean = bean;
        this.instance = instance;
        this.creationalContext = creationalContext;
        this.parentCreationalContext = parentCreationalContext;
        this.destroyed = new AtomicBoolean(false);
    }

    @Override
    public T get() {
        if (destroyed.get()) {
            throw new IllegalStateException("Instance already destroyed");
        }
        return instance;
    }

    @Override
    public InjectableBean<T> getBean() {
        return bean;
    }

    @Override
    public void destroy() {
        if (instance != null && destroyed.compareAndSet(false, true)) {
            if (bean.getScope().equals(Dependent.class)) {
                destroyInternal();
            } else {
                Arc.container().getActiveContext(bean.getScope()).destroy(bean);
            }
        }
    }

    protected void destroyInternal() {
        if (parentCreationalContext != null) {
            parentCreationalContext.release();
        } else {
            bean.destroy(instance, creationalContext);
        }
    }

    @Override
    public String toString() {
        return "InstanceHandleImpl [bean=" + bean + ", instance=" + instance + ", creationalContext=" + creationalContext
                + ", parentCreationalContext=" + parentCreationalContext + ", destroyed=" + destroyed + "]";
    }

}
