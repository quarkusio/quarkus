package io.quarkus.rest.client.reactive.runtime;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

@RequestScoped
public class HeaderContainer {

    private static final MultivaluedHashMap<String, String> EMPTY_MAP = new MultivaluedHashMap<>();
    private ContainerRequestContext requestContext;

    void setContainerRequestContext(ContainerRequestContext requestContext) {
        this.requestContext = requestContext;
    }

    public MultivaluedMap<String, String> getHeaders() {
        if (requestContext == null) {
            return EMPTY_MAP;
        } else {
            return requestContext.getHeaders();
        }
    }
}
