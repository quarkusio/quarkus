package io.quarkus.resteasy.reactive.server.test.simple;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TestRequestFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String previousFilterHeaderValue = requestContext.getHeaders().getFirst("filter-request");
        requestContext.getHeaders().putSingle("filter-request", previousFilterHeaderValue + "-default");
    }

}
