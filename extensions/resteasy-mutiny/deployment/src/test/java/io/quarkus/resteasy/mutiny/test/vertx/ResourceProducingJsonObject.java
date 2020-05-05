package io.quarkus.resteasy.mutiny.test.vertx;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
