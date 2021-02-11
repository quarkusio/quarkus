package io.quarkus.micrometer.runtime.binder.vertx;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;

public class VertxMeterBinderRestEasyContainerFilter implements ContainerRequestFilter {

    @Override
    public void filter(final ContainerRequestContext requestContext) {
        VertxMeterBinderContainerFilterUtil.doFilter(CDI.current().select(CurrentVertxRequest.class).get().getCurrent(),
                requestContext.getUriInfo());
    }
}
