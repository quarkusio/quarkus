package io.quarkus.arc.impl;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Consumer;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InstanceHandle;

abstract class AbstractInstanceHandle<T> implements InstanceHandle<T> {

    private static final Logger LOGGER = Logger.getLogger(AbstractInstanceHandle.class.getName());

    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<AbstractInstanceHandle> DESTROYED_UPDATER = AtomicIntegerFieldUpdater
            .newUpdater(AbstractInstanceHandle.class, "destroyed");

    private final InjectableBean<T> bean;
    private final CreationalContext<T> creationalContext;
    private final CreationalContext<?> parentCreationalContext;
    private final Consumer<T> destroyLogic;

    // values: 0="not destroyed", 1="destroyed"
    private volatile int destroyed;

    AbstractInstanceHandle(InjectableBean<T> bean, CreationalContext<T> creationalContext,
            CreationalContext<?> parentCreationalContext, Consumer<T> destroyLogic) {
        this.bean = bean;
        this.creationalContext = creationalContext;
        this.parentCreationalContext = parentCreationalContext;
        this.destroyLogic = destroyLogic;
    }

    @Override
    public T get() {
        if (destroyed != 0) {
            throw new IllegalStateException("Instance already destroyed");
        }
        return instanceInternal();
    }

    @Override
    public InjectableBean<T> getBean() {
        return bean;
    }

    protected abstract boolean isInstanceCreated();

    protected abstract T instanceInternal();

    @Override
    public void destroy() {
        if (isInstanceCreated() && DESTROYED_UPDATER.compareAndSet(this, 0, 1)) {
            if (destroyLogic != null) {
                destroyLogic.accept(instanceInternal());
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
            bean.destroy(instanceInternal(), creationalContext);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [bean=" + bean + ", destroyed=" + (destroyed != 0) + "]";
    }

}
