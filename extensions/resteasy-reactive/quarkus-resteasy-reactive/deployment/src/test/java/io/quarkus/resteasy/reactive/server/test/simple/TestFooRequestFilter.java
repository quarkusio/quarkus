package io.quarkus.resteasy.reactive.server.test.simple;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import io.vertx.core.http.HttpServerRequest;

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
