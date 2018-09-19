package org.jboss.protean.arc;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

public class RequestContext implements AlterableContext {

    // It's a normal scope so there may be no more than one mapped instance per contextual type per thread
    private final ThreadLocal<Map<Contextual<?>, InstanceHandleImpl<?>>> currentContext = new ThreadLocal<>();

    @Override
    public Class<? extends Annotation> getScope() {
        return RequestScoped.class;
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        Map<Contextual<?>, InstanceHandleImpl<?>> ctx = currentContext.get();
        if (ctx == null) {
            // Thread local not set - context is not active!
            throw new ContextNotActiveException();
        }
        InstanceHandleImpl<T> instance = (InstanceHandleImpl<T>) ctx.get(contextual);
        if (instance == null && creationalContext != null) {
            // Bean instance does not exist - create one if we have CreationalContext
            instance = new InstanceHandleImpl<T>((InjectableBean<T>) contextual, contextual.create(creationalContext), creationalContext);
            ctx.put(contextual, instance);
        }
        return instance != null ? instance.get() : null;
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        return get(contextual, null);
    }

    @Override
    public boolean isActive() {
        return currentContext.get() != null;
    }

    @Override
    public void destroy(Contextual<?> contextual) {
        Map<Contextual<?>, InstanceHandleImpl<?>> ctx = currentContext.get();
        if (ctx == null) {
            return;
        }
        InstanceHandleImpl<?> instance = ctx.remove(contextual);
        if (instance != null) {
            instance.destroy();
        }
    }

    public void activate() {
        currentContext.set(new HashMap<>());
    }

    public void invalidate() {
        Map<Contextual<?>, InstanceHandleImpl<?>> ctx = currentContext.get();
        if (ctx != null) {
            synchronized (ctx) {
                for (InstanceHandleImpl<?> instance : ctx.values()) {
                    try {
                        instance.destroy();
                    } catch (Exception e) {
                        throw new IllegalStateException("Unable to destroy instance" + instance.get(), e);
                    }
                }
            }
            ctx.clear();
        }
    }

    public void deactivate() {
        // TODO maybe change if context propagation is supported
        invalidate();
        currentContext.remove();
    }

}
