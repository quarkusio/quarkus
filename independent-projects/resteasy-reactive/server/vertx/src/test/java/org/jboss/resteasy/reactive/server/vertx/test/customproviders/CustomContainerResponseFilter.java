package org.jboss.resteasy.reactive.server.vertx.test.customproviders;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;
import org.jboss.resteasy.reactive.server.filters.PreventAbortResteasyReactiveContainerRequestContext;

public class CustomContainerResponseFilter {

    @ServerResponseFilter
    public void whatever(SimpleResourceInfo simplifiedResourceInfo, ContainerResponseContext responseContext,
            ContainerRequestContext requestContext, Throwable t) {
        assertTrue(
                PreventAbortResteasyReactiveContainerRequestContext.class.isAssignableFrom(requestContext.getClass()));
        assertNull(t);
        if (simplifiedResourceInfo != null) {
            responseContext.getHeaders().putSingle("java-method", simplifiedResourceInfo.getMethodName());
        }
    }
}
