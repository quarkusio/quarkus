package io.quarkus.it.smallrye.config;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

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
