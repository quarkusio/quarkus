package io.quarkus.resteasy.runtime;

import java.lang.annotation.Annotation;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.http.impl.HttpServerRequestInternal;
import io.vertx.ext.web.RoutingContext;

@SuppressWarnings("unused")
@QuarkusRestPathTemplate
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 10)
public class QuarkusRestPathTemplateInterceptor {
    @Inject
    CurrentVertxRequest request;

    @AroundInvoke
    Object restMethodInvoke(InvocationContext context) throws Exception {
        QuarkusRestPathTemplate annotation = getAnnotation(context);
        RoutingContext routingContext = null;
        try {
            routingContext = request.getCurrent();
        } catch (ContextNotActiveException ex) {
            // just leave routingContext as null
        }
        if ((annotation != null) && (routingContext != null)) {
            ((HttpServerRequestInternal) request.getCurrent().request()).context().putLocal("UrlPathTemplate",
                    annotation.value());
        }
        return context.proceed();
    }

    @SuppressWarnings("unchecked")
    static QuarkusRestPathTemplate getAnnotation(InvocationContext context) {
        Set<Annotation> annotations = (Set<Annotation>) context.getContextData()
                .get(ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS);

        for (Annotation a : annotations) {
            if (a instanceof QuarkusRestPathTemplate) {
                return (QuarkusRestPathTemplate) a;
            }
        }
        return null;
    }
}
