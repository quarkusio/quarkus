package org.jboss.resteasy.reactive.server.spi;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;

public interface ResteasyReactiveContainerResponseFilter extends ContainerResponseFilter {
    @Override
    default void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        filter((ResteasyReactiveContainerRequestContext) requestContext, responseContext);
    }

    void filter(ResteasyReactiveContainerRequestContext requestContext, ContainerResponseContext responseContext);
}
