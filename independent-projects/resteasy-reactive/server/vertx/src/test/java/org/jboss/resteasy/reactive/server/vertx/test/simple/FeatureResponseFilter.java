package org.jboss.resteasy.reactive.server.vertx.test.simple;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;

public class FeatureResponseFilter implements ContainerResponseFilter {

    private final String headerName;
    private final String headerValue;

    public FeatureResponseFilter(String headerName, String headerValue) {
        this.headerName = headerName;
        this.headerValue = headerValue;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        responseContext.getHeaders().add(headerName, headerValue);
    }
}
