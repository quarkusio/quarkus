package io.quarkus.rest.server.test.simple;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

@Provider
@Foo
@Priority(Priorities.USER - 1)
public class TestFooResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        String previousFilterHeaderValue = (String) responseContext.getHeaders().getFirst("filter-response");
        responseContext.getHeaders().putSingle("filter-response", previousFilterHeaderValue + "-foo");
    }

}
