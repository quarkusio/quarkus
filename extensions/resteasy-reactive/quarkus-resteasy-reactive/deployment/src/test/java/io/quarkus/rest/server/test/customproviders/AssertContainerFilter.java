package io.quarkus.rest.server.test.customproviders;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Request;

import org.jboss.resteasy.reactive.ContainerRequestFilter;
import org.jboss.resteasy.reactive.ContainerResponseFilter;
import org.jboss.resteasy.reactive.server.core.LazyMethod;
import org.jboss.resteasy.reactive.server.core.QuarkusRestSimplifiedResourceInfo;
import org.jboss.resteasy.reactive.server.jaxrs.QuarkusRestRequest;
import org.jboss.resteasy.reactive.server.spi.QuarkusRestContainerRequestContext;
import org.jboss.resteasy.reactive.server.spi.QuarkusRestContainerResponseContext;
import org.jboss.resteasy.reactive.server.spi.SimplifiedResourceInfo;

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

    @ContainerRequestFilter
    public void whatever(Request request, HttpServerRequest httpServerRequest, SimplifiedResourceInfo simplifiedResourceInfo,
            ResourceInfo resourceInfo, QuarkusRestContainerRequestContext quarkusRestContainerRequestContext,
            RoutingContext routingContext) {
        assertNotNull(someBean);
        assertTrue(QuarkusRestRequest.class.isAssignableFrom(request.getClass()));
        assertNotNull(httpServerRequest);
        assertTrue(QuarkusRestSimplifiedResourceInfo.class.isAssignableFrom(simplifiedResourceInfo.getClass()));
        assertTrue(LazyMethod.class.isAssignableFrom(resourceInfo.getClass()));
        assertNotNull(quarkusRestContainerRequestContext);
        assertNotNull(routingContext);
        COUNT.incrementAndGet();
    }

    @ContainerRequestFilter
    public void another() {
        assertNotNull(someBean);
        COUNT.incrementAndGet();
    }

    @ContainerResponseFilter
    public void response(QuarkusRestContainerRequestContext quarkusRestContainerRequestContext,
            QuarkusRestContainerResponseContext quarkusRestContainerResponseContext) {
        assertNotNull(someBean);
        assertNotNull(quarkusRestContainerRequestContext);
        assertNotNull(quarkusRestContainerResponseContext);
        COUNT.incrementAndGet();
    }
}
