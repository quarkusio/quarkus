package io.quarkus.spring.web.resteasy.classic.runtime;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;

public class ResponseStatusContainerResponseFilter implements ContainerResponseFilter {

    private final int defaultStatusCode;
    private final int newStatusCode;

    public ResponseStatusContainerResponseFilter(final int defaultStatusCode, final int newStatusCode) {
        this.defaultStatusCode = defaultStatusCode;
        this.newStatusCode = newStatusCode;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        if (responseContext.getStatus() == defaultStatusCode) {
            responseContext.setStatus(newStatusCode);
        }
    }
}
