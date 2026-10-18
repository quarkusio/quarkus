package io.quarkus.azure.functions.runtime;

import com.microsoft.azure.functions.internal.spi.middleware.Middleware;
import com.microsoft.azure.functions.internal.spi.middleware.MiddlewareChain;
import com.microsoft.azure.functions.internal.spi.middleware.MiddlewareContext;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;

public class QuarkusAzureFunctionsMiddleware implements Middleware {
    @Override
    public void invoke(MiddlewareContext middlewareContext, MiddlewareChain middlewareChain) throws Exception {
        ManagedContext requestContext = Arc.container().requestContext();
        boolean alreadyActive = requestContext.isActive();
        if (!alreadyActive) {
            requestContext.activate();
        }
        try {
            middlewareChain.doNext(middlewareContext);
        } finally {
            if (!alreadyActive && requestContext.isActive()) {
                requestContext.terminate();
            }
        }
    }
}
