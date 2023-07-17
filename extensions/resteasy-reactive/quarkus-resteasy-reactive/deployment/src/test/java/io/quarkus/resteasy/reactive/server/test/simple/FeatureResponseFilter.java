package io.quarkus.resteasy.reactive.server.test.simple;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;

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
