package org.jboss.resteasy.reactive.server.spi;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;

public interface ResteasyReactiveContainerRequestFilter extends ContainerRequestFilter {
    @Override
    default void filter(ContainerRequestContext requestContext) throws IOException {
        filter((ResteasyReactiveContainerRequestContext) requestContext);
    }

    void filter(ResteasyReactiveContainerRequestContext requestContext);
}
