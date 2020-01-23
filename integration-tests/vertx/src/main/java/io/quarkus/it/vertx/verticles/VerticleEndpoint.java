package io.quarkus.it.vertx.verticles;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.vertx.axle.core.Vertx;
import io.vertx.axle.core.eventbus.Message;

@Path("/verticles")
@Produces(MediaType.TEXT_PLAIN)
public class VerticleEndpoint {

    @Inject
    Vertx vertx;

    @GET
    @Path("/bare")
    public CompletionStage<String> bare() {
        return vertx.eventBus().<String> request("bare", "")
                .thenApply(Message::body);
    }

    @GET
    @Path("/bare-classname")
    public CompletionStage<String> bareWithClassName() {
        return vertx.eventBus().<String> request("bare-classname", "")
                .thenApply(Message::body);
    }

    @GET
    @Path("/rx")
    public CompletionStage<String> rx() {
        return vertx.eventBus().<String> request("rx", "")
                .thenApply(Message::body);
    }

    @GET
    @Path("/rx-classname")
    public CompletionStage<String> rxWithClassName() {
        return vertx.eventBus().<String> request("rx-classname", "")
                .thenApply(Message::body);
    }

}
