package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.Collections;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
    @Path("jsonObjectWrapper")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public JsonObjectWrapper jsonObjectWrapper(JsonObjectWrapper wrapper) {
        var payload = wrapper.payload;
        JsonObject result = new JsonObject();
        result.put("name", payload.getString("name"));
        result.put("age", 50);
        result.put("nested", new JsonObject(Collections.singletonMap("foo", "bar")));
        result.put("bools", new JsonArray().add(true));
        return new JsonObjectWrapper(result);
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

    @POST
    @Path("jsonArrayWrapper")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public JsonArrayWrapper jsonArrayWrapper(JsonArrayWrapper wrapper) {
        var payload = wrapper.payload;
        JsonArray result = payload.copy();
        result.add("last");
        return new JsonArrayWrapper(result);
    }

    public record JsonObjectWrapper(JsonObject payload) {
    }

    public record JsonArrayWrapper(JsonArray payload) {

    }
}
