package io.quarkus.resteasy.reactive.server.test.simple;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;

@Priority(Priorities.USER + 1000)
public class DynamicFeatureRequestFilterWithLowPriority implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String previousFilterHeaderValue = requestContext.getHeaders().getFirst("feature-filter-request");
        requestContext.getHeaders().putSingle("feature-filter-request", previousFilterHeaderValue + "-low");
    }
}
