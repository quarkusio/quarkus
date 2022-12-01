package io.quarkus.resteasy.runtime;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.annotation.Priority;
import javax.enterprise.context.ContextNotActiveException;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

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
