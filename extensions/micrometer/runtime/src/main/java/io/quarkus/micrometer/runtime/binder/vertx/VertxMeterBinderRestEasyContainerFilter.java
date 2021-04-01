package io.quarkus.micrometer.runtime.binder.vertx;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;

public class VertxMeterBinderRestEasyContainerFilter implements ContainerRequestFilter {

    @Inject
    CurrentVertxRequest currentVertxRequest;

    @Override
    public void filter(final ContainerRequestContext requestContext) {
        VertxMeterBinderContainerFilterUtil.doFilter(currentVertxRequest.getCurrent(),
                requestContext.getUriInfo());
    }
}
