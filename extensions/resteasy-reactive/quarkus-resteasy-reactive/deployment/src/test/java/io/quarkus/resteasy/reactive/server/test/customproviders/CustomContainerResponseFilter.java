package io.quarkus.resteasy.reactive.server.test.customproviders;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;
import org.jboss.resteasy.reactive.server.filters.PreventAbortResteasyReactiveContainerRequestContext;

public class CustomContainerResponseFilter {

    @ServerResponseFilter
    public void whatever(SimpleResourceInfo simplifiedResourceInfo, ContainerResponseContext responseContext,
            ContainerRequestContext requestContext, UriInfo uriInfo, Throwable t) {
        assertTrue(
                PreventAbortResteasyReactiveContainerRequestContext.class.isAssignableFrom(requestContext.getClass()));
        assertNull(t);
        assertNotNull(uriInfo);
        if (simplifiedResourceInfo != null) {
            responseContext.getHeaders().putSingle("java-method", simplifiedResourceInfo.getMethodName());
        }
    }
}
