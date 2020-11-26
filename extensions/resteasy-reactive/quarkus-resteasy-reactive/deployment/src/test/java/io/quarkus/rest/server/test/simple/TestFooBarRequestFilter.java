package io.quarkus.rest.server.test.simple;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

@Provider
@Foo
@Bar
@Priority(Priorities.USER + 2)
public class TestFooBarRequestFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String previousFilterHeaderValue = requestContext.getHeaders().getFirst("filter-request");
        requestContext.getHeaders().putSingle("filter-request", previousFilterHeaderValue + "-foobar");
    }

}
