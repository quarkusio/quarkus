package io.quarkus.signals.runtime.impl;

import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext.ContextState;
import io.quarkus.arc.ManagedContext;
import io.quarkus.signals.spi.ReceiverInterceptor;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;

/**
 * Built-in interceptor that activates a new CDI request context for each receiver invocation.
 */
@Identifier(ReceiverInterceptor.ID_REQUEST_CONTEXT)
@Singleton
public class RequestContextInterceptor implements ReceiverInterceptor {

    private static final Logger LOG = Logger.getLogger(RequestContextInterceptor.class);

    private final ManagedContext requestContext;

    public RequestContextInterceptor() {
        requestContext = Arc.container().requestContext();
    }

    @Override
    public Uni<Object> intercept(InterceptionContext context) {
        ContextState contextState = requestContext.activate();
        if (LOG.isDebugEnabled()) {
            LOG.debugf("Activated request context %s", toIdentityString(contextState));
        }
        return context.proceed().eventually(new Runnable() {
            @Override
            public void run() {
                // Destroy the specific ContextState rather than calling requestContext.terminate();
                // an async termination could race with SmallRye Context Propagation which may have already
                // restored a different context state (e.g. from the originating HTTP request) on this duplicated
                // Vert.x context, and terminate() would destroy that propagated state instead of ours
                requestContext.destroy(contextState);
                if (LOG.isDebugEnabled()) {
                    LOG.debugf("Destroyed request context %s", toIdentityString(contextState));
                }
            }
        });
    }

    static String toIdentityString(Object o) {
        return o.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }

}
