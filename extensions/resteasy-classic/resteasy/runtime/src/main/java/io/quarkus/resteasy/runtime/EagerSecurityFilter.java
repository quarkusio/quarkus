package io.quarkus.resteasy.runtime;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.vertx.http.runtime.security.EagerSecurityInterceptorStorage;
import io.vertx.ext.web.RoutingContext;

@Priority(Priorities.AUTHENTICATION)
@Provider
public class EagerSecurityFilter implements ContainerRequestFilter {

    private static final Consumer<RoutingContext> NULL_SENTINEL = new Consumer<RoutingContext>() {
        @Override
        public void accept(RoutingContext routingContext) {

        }
    };
    private final Map<MethodDescription, Consumer<RoutingContext>> cache = new HashMap<>();

    @Context
    ResourceInfo resourceInfo;

    @Inject
    EagerSecurityInterceptorStorage interceptorStorage;

    @Inject
    RoutingContext routingContext;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        var description = MethodDescription.ofMethod(resourceInfo.getResourceMethod());
        var interceptor = cache.get(description);

        if (interceptor == NULL_SENTINEL) {
            return;
        } else if (interceptor != null) {
            interceptor.accept(routingContext);
        }

        interceptor = interceptorStorage.getInterceptor(description);
        if (interceptor == null) {
            cache.put(description, NULL_SENTINEL);
        } else {
            cache.put(description, interceptor);
            interceptor.accept(routingContext);
        }
    }
}
