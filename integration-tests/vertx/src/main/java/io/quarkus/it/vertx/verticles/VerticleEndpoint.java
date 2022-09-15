package io.quarkus.it.vertx.verticles;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

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

    @GET
    @Path("/mdc")
    public Uni<String> mdc(@QueryParam("value") String value) {
        return vertx.eventBus().<String> request("mdc", value)
                .onItem().transform(Message::body);
    }

}
