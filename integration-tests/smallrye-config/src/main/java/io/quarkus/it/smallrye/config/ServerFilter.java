package io.quarkus.it.smallrye.config;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;

@Priority(1001)
public class ServerFilter implements ContainerResponseFilter {
    private final String version;

    public ServerFilter(String version) {
        this.version = version;
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) {
        containerResponseContext.getHeaders().add("X-VERSION", version);
    }
}
