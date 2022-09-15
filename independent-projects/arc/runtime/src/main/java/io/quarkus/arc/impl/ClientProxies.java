package io.quarkus.arc.impl;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.Contextual;
import java.util.List;

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
        List<InjectableContext> contexts = Arc.container().getContexts(bean.getScope());
        T result = null;
        if (contexts.size() == 1) {
            result = contexts.get(0).getIfActive(bean, ClientProxies::newCreationalContext);
        } else {
            InjectableContext selectedContext = null;
            for (int i = 0; i < contexts.size(); i++) {
                InjectableContext context = contexts.get(i);
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
        }
        if (result == null) {
            String msg = String.format(
                    "%s context was not active when trying to obtain a bean instance for a client proxy of %s",
                    bean.getScope().getSimpleName(), bean);
            if (bean.getScope().equals(RequestScoped.class)) {
                msg += "\n\t- you can activate the request context for a specific method using the @ActivateRequestContext interceptor binding";
            }
            throw new ContextNotActiveException(msg);
        }
        return result;
    }

    private static <T> CreationalContextImpl<T> newCreationalContext(Contextual<T> contextual) {
        return new CreationalContextImpl<>(contextual);
    }

}
