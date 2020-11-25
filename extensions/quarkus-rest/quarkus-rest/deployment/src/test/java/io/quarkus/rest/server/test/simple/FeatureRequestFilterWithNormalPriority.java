package io.quarkus.rest.server.test.simple;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

public class FeatureRequestFilterWithNormalPriority implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String previousFilterHeaderValue = requestContext.getHeaders().getFirst("feature-filter-request");
        requestContext.getHeaders().putSingle("feature-filter-request", previousFilterHeaderValue + "-default");
    }
}
