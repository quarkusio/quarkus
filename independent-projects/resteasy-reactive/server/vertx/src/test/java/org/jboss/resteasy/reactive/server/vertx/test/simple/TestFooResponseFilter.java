package org.jboss.resteasy.reactive.server.vertx.test.simple;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

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
