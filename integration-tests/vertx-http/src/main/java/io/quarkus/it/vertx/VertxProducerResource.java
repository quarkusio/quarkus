package io.quarkus.it.vertx;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

@Path("/")
public class VertxProducerResource {

    @Inject
    Vertx vertx;

    @Inject
    Router router;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String test() {
        if (vertx == null) {
            throw new NullPointerException("vert.x instance should have been injected");
        }
        if (router == null) {
            throw new NullPointerException("router instance should have been injected");
        }
        return "vert.x has been injected";
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public String testBody(String body) {
        if (vertx == null) {
            throw new NullPointerException("vert.x instance should have been injected");
        }
        if (router == null) {
            throw new NullPointerException("router instance should have been injected");
        }
        return body;
    }

}
