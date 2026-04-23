package io.quarkus.signals.runtime.impl;

import jakarta.inject.Singleton;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.signals.spi.ReceiverInterceptor;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;

/**
 * Built-in interceptor that activates a new CDI request context for each receiver invocation.
 */
@Identifier(ReceiverInterceptor.ID_REQUEST_CONTEXT)
@Singleton
public class DefaultRequestContextInterceptor implements ReceiverInterceptor {

    private final ManagedContext requestContext;

    public DefaultRequestContextInterceptor() {
        requestContext = Arc.container().requestContext();
    }

    @Override
    public Uni<Object> intercept(InterceptionContext context) {
        requestContext.activate();
        return context.proceed().eventually(requestContext::terminate);
    }

}
