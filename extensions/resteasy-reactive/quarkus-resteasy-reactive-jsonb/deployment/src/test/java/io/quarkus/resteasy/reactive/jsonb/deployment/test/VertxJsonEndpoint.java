package io.quarkus.resteasy.reactive.jsonb.deployment.test;

import java.util.Collections;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@Path("vertx")
public class VertxJsonEndpoint {

    @POST
    @Path("jsonObject")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public JsonObject jsonObject(JsonObject input) {
        JsonObject result = new JsonObject();
        result.put("name", input.getString("name"));
        result.put("age", 50);
        result.put("nested", new JsonObject(Collections.singletonMap("foo", "bar")));
        result.put("bools", new JsonArray().add(true));
        return result;
    }

    @POST
    @Path("jsonArray")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public JsonArray jsonArray(JsonArray input) {
        JsonArray result = input.copy();
        result.add("last");
        return result;
    }
}
