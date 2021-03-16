package io.quarkus.arc.impl;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InstanceHandle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import org.jboss.logging.Logger;

/**
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
class InstanceHandleImpl<T> implements InstanceHandle<T> {

    private static final Logger LOGGER = Logger.getLogger(InstanceHandleImpl.class.getName());

    @SuppressWarnings("unchecked")
    public static final <T> InstanceHandle<T> unavailable() {
        return (InstanceHandle<T>) UNAVAILABLE;
    }

    static final InstanceHandleImpl<Object> UNAVAILABLE = new InstanceHandleImpl<Object>(null, null, null, null, null);

    private final InjectableBean<T> bean;
    private final T instance;
    private final CreationalContext<T> creationalContext;
    private final CreationalContext<?> parentCreationalContext;
    private final AtomicBoolean destroyed;
    private final Consumer<T> destroyLogic;

    InstanceHandleImpl(InjectableBean<T> bean, T instance, CreationalContext<T> creationalContext) {
        this(bean, instance, creationalContext, null, null);
    }

    InstanceHandleImpl(InjectableBean<T> bean, T instance, CreationalContext<T> creationalContext,
            CreationalContext<?> parentCreationalContext, Consumer<T> destroyLogic) {
        this.bean = bean;
        this.instance = instance;
        this.creationalContext = creationalContext;
        this.parentCreationalContext = parentCreationalContext;
        this.destroyed = new AtomicBoolean(false);
        this.destroyLogic = destroyLogic;
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
            if (destroyLogic != null) {
                destroyLogic.accept(instance);
            } else {
                if (bean.getScope().equals(Dependent.class)) {
                    destroyInternal();
                } else {
                    InjectableContext context = Arc.container().getActiveContext(bean.getScope());
                    if (context == null) {
                        throw new ContextNotActiveException(
                                "Cannot destroy instance of " + bean + " - no active context found for: " + bean.getScope());
                    }
                    context.destroy(bean);
                }
            }
        }
    }

    protected void destroyInternal() {
        if (parentCreationalContext != null) {
            parentCreationalContext.release();
        } else {
            try {
                bean.destroy(instance, creationalContext);
            } catch (Throwable t) {
                String msg = "Error occurred while destroying instance of bean [%s]";
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.errorf(t, msg, bean.getClass().getName());
                } else {
                    LOGGER.errorf(msg + ": %s", bean.getClass().getName(), t);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "InstanceHandleImpl [bean=" + bean + ", instance=" + instance + ", creationalContext=" + creationalContext
                + ", parentCreationalContext=" + parentCreationalContext + ", destroyed=" + destroyed + "]";
    }

}
