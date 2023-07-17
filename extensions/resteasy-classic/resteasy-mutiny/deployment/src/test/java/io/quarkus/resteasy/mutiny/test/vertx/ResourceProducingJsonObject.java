package io.quarkus.resteasy.mutiny.test.vertx;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

@Path("/vertx")
@Produces(MediaType.APPLICATION_JSON)
public class ResourceProducingJsonObject {

    @GET
    @Path("{name}/multi")
    public Multi<JsonObject> multi(@PathParam("name") String name) {
        return Multi.createFrom().items(new JsonObject().put("Hello", name));
    }

    @GET
    @Path("{name}/uni")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<JsonObject> uni(@PathParam("name") String name) {
        return Uni.createFrom().item(new JsonObject().put("Hello", name));
    }
}
