package io.quarkus.rest.client.reactive.runtime;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;

import io.quarkus.arc.Arc;

@SuppressWarnings("unused")
public class HeaderCapturingServerFilter implements ContainerRequestFilter {
    private final HeaderContainer headerContainer;

    public HeaderCapturingServerFilter() {
        headerContainer = Arc.container().instance(HeaderContainer.class).get();
    }

    public void filter(ContainerRequestContext requestContext) {
        if (headerContainer != null) {
            headerContainer.setContainerRequestContext(requestContext);
        }
    }
}
