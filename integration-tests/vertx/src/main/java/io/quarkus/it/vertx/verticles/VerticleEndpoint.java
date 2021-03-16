package io.quarkus.it.vertx.verticles;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.Message;

@Path("/verticles")
@Produces(MediaType.TEXT_PLAIN)
public class VerticleEndpoint {

    @Inject
    Vertx vertx;

    @GET
    @Path("/bare")
    public Uni<String> bare() {
        return vertx.eventBus().<String> request("bare", "")
                .onItem().transform(Message::body);
    }

    @GET
    @Path("/bare-classname")
    public Uni<String> bareWithClassName() {
        return vertx.eventBus().<String> request("bare-classname", "")
                .onItem().transform(Message::body);
    }

    @GET
    @Path("/mutiny")
    public Uni<String> mutiny() {
        return vertx.eventBus().<String> request("mutiny", "")
                .onItem().transform(Message::body);
    }

    @GET
    @Path("/mutiny-classname")
    public Uni<String> mutinyWithClassName() {
        return vertx.eventBus().<String> request("mutiny-classname", "")
                .onItem().transform(Message::body);
    }

}
