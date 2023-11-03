package io.quarkus.virtual.disabled;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.logmanager.MDC;

import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.Vertx;

@Path("/filter")
public class FilteredResource {

    @Inject
    Counter counter;

    @GET
    @RunOnVirtualThread
    public Response filtered() {
        VirtualThreadsAssertions.assertWorkerOrEventLoopThread();

        // Request scope
        assert counter.increment() == 2;

        // DC
        assert Vertx.currentContext().getLocal("filter").equals("test");
        Vertx.currentContext().putLocal("test", "test test");

        // MDC
        assert MDC.get("mdc").equals("test");
        MDC.put("mdc", "test test");

        return Response.ok()
                .header("X-filter", "true")
                .entity("ok")
                .build();
    }
}
