package io.quarkus.resteasy.reactive.client.timeout;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.vertx.core.Vertx;

@Path("/")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public class Resource {

    @Inject
    Vertx vertx;

    @Path("/slow")
    @GET
    public String slow() throws InterruptedException {
        Thread.sleep(5000L);
        return "slow-response";
    }

    @Path("/fast")
    @GET
    public CompletionStage<String> fast() {
        return CompletableFuture.completedFuture("fast-response");
    }

}
