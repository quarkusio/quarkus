package io.quarkus.reactive.h2.client.deployment;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.vertx.jdbcclient.JDBCPool;

@Path("/dev")
public class DevModeResource {

    @Inject
    JDBCPool client;

    @GET
    @Path("/dbname")
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<Response> getErrorMessage() {
        CompletableFuture<Response> future = new CompletableFuture<>();
        client.query("SELECT CATALOG_NAME FROM INFORMATION_SCHEMA.SCHEMATA").execute(
                ar -> future.complete(Response.ok(ar.result().iterator().next().getString("CATALOG_NAME")).build()));
        return future;
    }
}
