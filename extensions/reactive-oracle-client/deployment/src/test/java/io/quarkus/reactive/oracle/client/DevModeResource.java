package io.quarkus.reactive.oracle.client;

import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.vertx.oracleclient.OraclePool;

@Path("/dev")
public class DevModeResource {

    @Inject
    OraclePool client;

    @GET
    @Path("/error")
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<Response> checkConnectionFailure() throws SQLException {
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.query("SELECT 1 FROM DUAL").execute(ar -> {
            Class<?> expectedExceptionClass = SQLException.class;
            if (ar.succeeded()) {
                future.complete(Response.serverError().entity("Expected SQL query to fail").build());
            } else if (!expectedExceptionClass.isAssignableFrom(ar.cause().getClass())) {
                future.complete(Response.serverError()
                        .entity("Expected " + expectedExceptionClass + ", got " + ar.cause().getClass()).build());
            } else {
                future.complete(Response.ok().build());
            }
        });
        return future;
    }

    @GET
    @Path("/connected")
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<Response> checkConnectionSuccess() throws SQLException {
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.query("SELECT 1 FROM DUAL").execute(ar -> {
            if (ar.succeeded()) {
                future.complete(Response.ok().build());
            } else {
                future.complete(Response.serverError().entity(ar.cause().getMessage()).build());
            }
        });
        return future;
    }
}
