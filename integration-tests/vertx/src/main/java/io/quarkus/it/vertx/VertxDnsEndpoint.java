package io.quarkus.it.vertx;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.vertx.axle.core.Vertx;

@Path("/dns")
@Produces(MediaType.TEXT_PLAIN)
public class VertxDnsEndpoint {

    @Inject
    Vertx vertx;

    @GET
    public CompletionStage<String> resolve() {
        return vertx.createDnsClient(53, "8.8.8.8").lookup("quarkus.io");
    }
}
