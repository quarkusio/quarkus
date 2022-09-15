package org.jboss.resteasy.reactive.server.vertx.test.customproviders;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Request;
import java.util.List;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveSimplifiedResourceInfo;
import org.jboss.resteasy.reactive.server.filters.PreventAbortResteasyReactiveContainerRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.RequestImpl;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;

/**
 * Used only to ensure that the proper types are passed to the method and that CDI integrations work properly
 */
public class AssertContainerFilter {

    private final SomeBean someBean;

    public AssertContainerFilter(SomeBean someBean) {
        this.someBean = someBean;
    }

    @ServerRequestFilter
    public void whatever(Request request, HttpServerRequest httpServerRequest, SimpleResourceInfo simplifiedResourceInfo,
            ResourceInfo resourceInfo, ResteasyReactiveContainerRequestContext resteasyReactiveContainerRequestContext,
            ContainerRequestContext containerRequestContext, RoutingContext routingContext) {
        assertNotNull(someBean);
        assertTrue(RequestImpl.class.isAssignableFrom(request.getClass()));
        assertNotNull(httpServerRequest);
        assertTrue(ResteasyReactiveSimplifiedResourceInfo.class.isAssignableFrom(simplifiedResourceInfo.getClass()));
        assertTrue(ResteasyReactiveResourceInfo.class.isAssignableFrom(resourceInfo.getClass()));
        assertNotNull(resteasyReactiveContainerRequestContext);
        assertTrue(
                PreventAbortResteasyReactiveContainerRequestContext.class.isAssignableFrom(containerRequestContext.getClass()));
        assertNotNull(routingContext);
        httpServerRequest.response().headers().set("h1", "true");
    }

    @ServerRequestFilter
    public void another(HttpServerRequest httpServerRequest) {
        assertNotNull(someBean);
        httpServerRequest.response().headers().set("h2", "true");
    }

    @ServerResponseFilter
    public void response(ResteasyReactiveContainerRequestContext resteasyReactiveContainerRequestContext,
            ContainerResponseContext containerResponseContext) {
        assertNotNull(someBean);
        assertNotNull(resteasyReactiveContainerRequestContext);
        assertNotNull(containerResponseContext);
        containerResponseContext.getHeaders().put("h3", List.of("true"));
    }
}
