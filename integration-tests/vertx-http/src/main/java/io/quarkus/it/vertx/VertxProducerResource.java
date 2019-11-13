package io.quarkus.it.vertx;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
