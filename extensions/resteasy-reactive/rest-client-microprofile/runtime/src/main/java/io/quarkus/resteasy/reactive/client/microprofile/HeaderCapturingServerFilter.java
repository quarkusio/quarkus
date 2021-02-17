package io.quarkus.resteasy.reactive.client.microprofile;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import io.quarkus.arc.Arc;

@SuppressWarnings("unused")
public class HeaderCapturingServerFilter implements ContainerRequestFilter {

    public void filter(ContainerRequestContext requestContext) {
        HeaderContainer instance = Arc.container().instance(HeaderContainer.class).get();
        if (instance != null) {
            instance.setContainerRequestContext(requestContext);
        }
    }
}
