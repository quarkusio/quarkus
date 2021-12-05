package org.jboss.resteasy.reactive.server.vertx.test.simple;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

@Provider
public class TestRequestFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String previousFilterHeaderValue = requestContext.getHeaders().getFirst("filter-request");
        requestContext.getHeaders().putSingle("filter-request", previousFilterHeaderValue + "-default");
    }

}
