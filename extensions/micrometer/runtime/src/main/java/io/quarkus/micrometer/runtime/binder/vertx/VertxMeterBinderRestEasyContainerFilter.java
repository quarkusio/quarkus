package io.quarkus.micrometer.runtime.binder.vertx;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import io.vertx.ext.web.RoutingContext;

public class VertxMeterBinderRestEasyContainerFilter implements ContainerRequestFilter {

    @Inject
    RoutingContext routingContext;

    @Override
    public void filter(final ContainerRequestContext requestContext) {
        VertxMeterBinderContainerFilterUtil.doFilter(routingContext, requestContext.getUriInfo());
    }
}
