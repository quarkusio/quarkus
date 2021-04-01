package io.quarkus.micrometer.runtime.binder.vertx;

import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import io.vertx.ext.web.RoutingContext;

public class VertxMeterBinderRestEasyReactiveContainerFilter {

    @Inject
    RoutingContext routingContext;

    @ServerRequestFilter
    public void filter(UriInfo uriInfo, RoutingContext routingContext) {
        VertxMeterBinderContainerFilterUtil.doFilter(routingContext, uriInfo);
    }
}
