package io.quarkus.rest.server.runtime.spi;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

public interface QuarkusRestContainerResponseFilter extends ContainerResponseFilter {
    @Override
    default void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        filter((QuarkusRestContainerRequestContext) requestContext, (QuarkusRestContainerResponseContext) responseContext);
    }

    public void filter(QuarkusRestContainerRequestContext requestContext, QuarkusRestContainerResponseContext responseContext);
}
