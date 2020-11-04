package io.quarkus.arc.impl;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.Contextual;

public final class ClientProxies {

    private ClientProxies() {
    }

    public static <T> T getApplicationScopedDelegate(InjectableContext applicationContext, InjectableBean<T> bean) {
        T result = applicationContext.get(bean);
        if (result == null) {
            result = applicationContext.get(bean, newCreationalContext(bean));
        }
        return result;
    }

    public static <T> T getDelegate(InjectableBean<T> bean) {
        Iterable<InjectableContext> contexts = Arc.container().getContexts(bean.getScope());
        T result = null;
        InjectableContext selectedContext = null;
        for (InjectableContext context : contexts) {
            if (result != null) {
                if (context.isActive()) {
                    throw new IllegalArgumentException(
                            "More than one context object for the given scope: " + selectedContext + " " + context);
                }
            } else {
                result = context.getIfActive(bean, ClientProxies::newCreationalContext);
                if (result != null) {
                    selectedContext = context;
                }
            }
        }
        if (result == null) {
            throw new ContextNotActiveException();
        }
        return result;
    }

    private static <T> CreationalContextImpl<T> newCreationalContext(Contextual<T> contextual) {
        return new CreationalContextImpl<>(contextual);
    }

}
