package io.quarkus.resteasy.test;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@Path("/json")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JsonResource {

    @Path("/array")
    @POST
    public JsonArray array(JsonArray array) {
        return array.add("test");
    }

    @Path("/obj")
    @POST
    public JsonObject obj(JsonObject array) {
        return array.put("test", "testval");
    }
}
