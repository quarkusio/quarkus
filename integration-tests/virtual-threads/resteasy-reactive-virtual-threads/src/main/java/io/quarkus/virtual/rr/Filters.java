package io.quarkus.virtual.rr;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;

import org.jboss.logmanager.MDC;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.vertx.core.Vertx;

public class Filters {
    @ServerRequestFilter(nonBlocking = true)
    public void request(ContainerRequestContext requestContext) {
        if (requestContext.getUriInfo().getPath().contains("/filter")) {
            VirtualThreadsAssertions.assertWorkerOrEventLoopThread();
            MDC.put("mdc", "test");
            CDI.current().select(Counter.class).get().increment();
            Vertx.currentContext().putLocal("filter", "test");
        }
    }

    @ServerResponseFilter
    public void getFilter(ContainerResponseContext responseContext) {
        if (responseContext.getHeaders().get("X-filter") != null) {
            VirtualThreadsAssertions.assertEverything();
            // the request filter, the method, and here.
            assert CDI.current().select(Counter.class).get().increment() == 4;
            assert Vertx.currentContext().getLocal("test").equals("test test");
            assert MDC.get("mdc").equals("test test");
        }
    }

}
