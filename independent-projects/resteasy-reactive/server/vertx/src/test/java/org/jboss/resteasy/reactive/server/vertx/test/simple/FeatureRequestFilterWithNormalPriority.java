package org.jboss.resteasy.reactive.server.vertx.test.simple;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;

public class FeatureRequestFilterWithNormalPriority implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String previousFilterHeaderValue = requestContext.getHeaders().getFirst("feature-filter-request");
        requestContext.getHeaders().putSingle("feature-filter-request", previousFilterHeaderValue + "-default");
    }
}
