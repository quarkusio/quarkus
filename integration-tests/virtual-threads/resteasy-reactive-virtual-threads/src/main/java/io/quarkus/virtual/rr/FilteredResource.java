package io.quarkus.virtual.rr;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.logmanager.MDC;

import io.quarkus.arc.Arc;
import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.quarkus.virtual.threads.VirtualThreads;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.Vertx;

@Path("/filter")
public class FilteredResource {

    @Inject
    Counter counter;

    @Inject
    @VirtualThreads
    ExecutorService vt;

    @GET
    @RunOnVirtualThread
    public Response filtered() throws ExecutionException, InterruptedException {
        VirtualThreadsAssertions.assertEverything();

        // Request scope
        assert counter.increment() == 2;

        // Request scope propagated
        assert vt.submit(() -> counter.increment()).get() == 3;

        // Request scope active
        assert Arc.container().requestContext().isActive();
        assert vt.submit(() -> Arc.container().requestContext().isActive()).get();

        CompletableFuture<Boolean> requestContextActive = new CompletableFuture<>();
        vt.execute(() -> requestContextActive.complete(Arc.container().requestContext().isActive()));
        assert requestContextActive.get();

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
