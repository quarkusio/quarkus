package io.quarkus.arc.impl;

import java.util.List;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.ConversationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.context.spi.Contextual;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;

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

    // This method is only used if a single context is registered for the given scope
    public static <T> T getSingleContextDelegate(InjectableContext context, InjectableBean<T> bean) {
        T result = context.getIfActive(bean, ClientProxies::newCreationalContext);
        if (result == null) {
            throw notActive(bean);
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
            throw notActive(bean);
        }
        return result;
    }

    private static ContextNotActiveException notActive(InjectableBean<?> bean) {
        String msg = String.format(
                "%s context was not active when trying to obtain a bean instance for a client proxy of %s",
                bean.getScope().getSimpleName(), bean);
        if (bean.getScope().equals(RequestScoped.class)) {
            msg += "\n\t- you can activate the request context for a specific method using the @ActivateRequestContext interceptor binding";
        } else if (bean.getScope().equals(SessionScoped.class)) {
            msg += "\n\t- @SessionScoped is not supported by default. However, a Quarkus extension implementing session context can be used to enable this functionality (such as Undertow extension).";
        } else if (bean.getScope().equals(ConversationScoped.class)) {
            msg += "\n\t- @ConversationScoped is not supported in Quarkus or CDI Lite. However, users are still allowed supply their custom context implementation.";
        }
        return new ContextNotActiveException(msg);
    }

    private static <T> CreationalContextImpl<T> newCreationalContext(Contextual<T> contextual) {
        return new CreationalContextImpl<>(contextual);
    }

}
