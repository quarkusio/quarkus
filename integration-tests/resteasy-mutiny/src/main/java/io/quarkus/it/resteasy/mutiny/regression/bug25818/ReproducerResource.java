package io.quarkus.it.resteasy.mutiny.regression.bug25818;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

@Path("/reproducer/25818")
public class ReproducerResource {

    private final Logger logger = Logger.getLogger(ReproducerResource.class);

    @Inject
    BlockingService service;

    private void addToContext() {
        Vertx.currentContext().putLocal("hello-target", "you");
    }

    @GET
    @Path("/worker-pool")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> workerPool() {
        logger.info("worker pool endpoint");
        addToContext();
        return Uni.createFrom()
                .item(service::getBlocking)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @GET
    @Path("/default-executor")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> defaultExecutor() {
        logger.info("default executor endpoint");
        addToContext();
        return Uni.createFrom()
                .item(service::getBlocking)
                .runSubscriptionOn(Infrastructure.getDefaultExecutor());
    }

    @GET
    @Path("/worker-pool-submit")
    public Uni<String> workerPoolSubmit() {
        Vertx.currentContext().putLocal("yolo", "yolo");
        return Uni.createFrom().emitter(emitter -> {
            Infrastructure.getDefaultWorkerPool().submit(() -> {
                Context ctx = Vertx.currentContext();
                if (ctx != null) {
                    emitter.complete("yolo -> " + ctx.getLocal("yolo"));
                } else {
                    emitter.complete("Context was null");
                }
            });
        });
    }

    @GET
    @Path("/worker-pool-schedule")
    public Uni<String> workerPoolSchedule() {
        Vertx.currentContext().putLocal("yolo", "yolo");
        return Uni.createFrom().emitter(emitter -> {
            Infrastructure.getDefaultWorkerPool().schedule(() -> {
                Context ctx = Vertx.currentContext();
                if (ctx != null) {
                    emitter.complete("yolo -> " + ctx.getLocal("yolo"));
                } else {
                    emitter.complete("Context was null");
                }
            }, 25, TimeUnit.MILLISECONDS);
        });
    }
}
