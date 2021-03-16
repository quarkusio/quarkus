package io.quarkus.reactive.pg.client;

import java.net.ConnectException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.vertx.pgclient.PgPool;

@Path("/dev")
public class DevModeResource {

    @Inject
    PgPool client;

    @GET
    @Path("/error")
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<Response> getErrorMessage() {
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.query("SELECT 1").execute(ar -> {
            Class<?> expectedExceptionClass = ConnectException.class;
            if (ar.succeeded()) {
                future.complete(Response.serverError().entity("Expected SQL query to fail").build());
            } else if (!expectedExceptionClass.isAssignableFrom(ar.cause().getClass())) {
                ar.cause().printStackTrace();
                future.complete(Response.serverError()
                        .entity("Expected " + expectedExceptionClass + ", got " + ar.cause().getClass()).build());
            } else {
                future.complete(Response.ok(ar.cause().getMessage()).build());
            }
        });
        return future;
    }
}
