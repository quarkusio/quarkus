package io.quarkus.rest.test.customproviders;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Request;

import io.quarkus.rest.ContainerRequestFilter;
import io.quarkus.rest.ContainerResponseFilter;
import io.quarkus.rest.runtime.core.LazyMethod;
import io.quarkus.rest.runtime.core.QuarkusRestSimplifiedResourceInfo;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestRequest;
import io.quarkus.rest.runtime.spi.QuarkusRestContainerRequestContext;
import io.quarkus.rest.runtime.spi.QuarkusRestContainerResponseContext;
import io.quarkus.rest.runtime.spi.SimplifiedResourceInfo;
import io.vertx.core.http.HttpServerRequest;

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
            ResourceInfo resourceInfo, QuarkusRestContainerRequestContext quarkusRestContainerRequestContext) {
        assertNotNull(someBean);
        assertTrue(QuarkusRestRequest.class.isAssignableFrom(request.getClass()));
        assertNotNull(httpServerRequest);
        assertTrue(QuarkusRestSimplifiedResourceInfo.class.isAssignableFrom(simplifiedResourceInfo.getClass()));
        assertTrue(LazyMethod.class.isAssignableFrom(resourceInfo.getClass()));
        assertNotNull(quarkusRestContainerRequestContext);
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
