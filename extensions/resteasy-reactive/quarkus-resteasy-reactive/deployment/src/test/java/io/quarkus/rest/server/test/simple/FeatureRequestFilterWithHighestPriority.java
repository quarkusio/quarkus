package io.quarkus.rest.server.test.simple;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

@Priority(Priorities.AUTHENTICATION)
public class FeatureRequestFilterWithHighestPriority implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        requestContext.getHeaders().add("feature-filter-request", "authentication");
    }
}
