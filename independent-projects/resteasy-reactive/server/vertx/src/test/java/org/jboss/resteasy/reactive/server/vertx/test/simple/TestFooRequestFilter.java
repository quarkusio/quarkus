package org.jboss.resteasy.reactive.server.vertx.test.simple;

import io.vertx.core.http.HttpServerRequest;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@Foo
@Priority(Priorities.USER - 1)
public class TestFooRequestFilter implements ContainerRequestFilter {

    @Context
    HttpServerRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String previousFilterHeaderValue = requestContext.getHeaders().getFirst("filter-request");
        requestContext.getHeaders().putSingle("filter-request", previousFilterHeaderValue + "-foo");
    }

}
