package io.quarkus.rest.server.test.customproviders;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Request;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.SimplifiedResourceInfo;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveSimplifiedResourceInfo;
import org.jboss.resteasy.reactive.server.jaxrs.QuarkusRestRequest;
import org.jboss.resteasy.reactive.server.spi.LazyMethod;
import org.jboss.resteasy.reactive.server.spi.QuarkusRestContainerRequestContext;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

/**
 * Used only to ensure that the proper types are passed to the method and that CDI integrations work properly
 */
public class AssertContainerFilter {

    private final SomeBean someBean;

    public static final AtomicInteger COUNT = new AtomicInteger(0);

    public AssertContainerFilter(SomeBean someBean) {
        this.someBean = someBean;
    }

    @ServerRequestFilter
    public void whatever(Request request, HttpServerRequest httpServerRequest, SimplifiedResourceInfo simplifiedResourceInfo,
            ResourceInfo resourceInfo, QuarkusRestContainerRequestContext quarkusRestContainerRequestContext,
            RoutingContext routingContext) {
        assertNotNull(someBean);
        assertTrue(QuarkusRestRequest.class.isAssignableFrom(request.getClass()));
        assertNotNull(httpServerRequest);
        assertTrue(ResteasyReactiveSimplifiedResourceInfo.class.isAssignableFrom(simplifiedResourceInfo.getClass()));
        assertTrue(LazyMethod.class.isAssignableFrom(resourceInfo.getClass()));
        assertNotNull(quarkusRestContainerRequestContext);
        assertNotNull(routingContext);
        COUNT.incrementAndGet();
    }

    @ServerRequestFilter
    public void another() {
        assertNotNull(someBean);
        COUNT.incrementAndGet();
    }

    @ServerResponseFilter
    public void response(QuarkusRestContainerRequestContext quarkusRestContainerRequestContext,
            ContainerResponseContext containerResponseContext) {
        assertNotNull(someBean);
        assertNotNull(quarkusRestContainerRequestContext);
        assertNotNull(containerResponseContext);
        COUNT.incrementAndGet();
    }
}
