package io.quarkus.it.vertx;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.ws.rs.*;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * @author Thomas Ssegismont
 */
@Path("/json-bodies")
public class JsonTestResource {

    @GET
    @Path("/json/sync")
    @Produces(APPLICATION_JSON)
    public JsonObject jsonSync() {
        return new JsonObject().put("Hello", "World");
    }

    @POST
    @Path("/json/sync")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    public String jsonSync(JsonObject jsonObject) {
        return "Hello " + jsonObject.getString("Hello");
    }

    @GET
    @Path("/array/sync")
    @Produces(APPLICATION_JSON)
    public JsonArray arraySync() {
        return new JsonArray().add("Hello").add("World");
    }

    @POST
    @Path("/array/sync")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    public String arraySync(JsonArray jsonArray) {
        return jsonArray.stream().map(String.class::cast).collect(Collectors.joining(" "));
    }

    @GET
    @Path("/json/async")
    @Produces(APPLICATION_JSON)
    public CompletionStage<JsonObject> jsonAsync() {
        return CompletableFuture.completedFuture(new JsonObject().put("Hello", "World"));
    }

    @GET
    @Path("/array/async")
    @Produces(APPLICATION_JSON)
    public CompletionStage<JsonArray> arrayAsync() {
        return CompletableFuture.completedFuture(new JsonArray().add("Hello").add("World"));
    }

    @GET
    @Path("/json/mapping")
    @Produces(APPLICATION_JSON)
    public String getPet() {
        // This test check that the Jackson mapping (used by Json.encode) works.
        return Json.encode(new Person("jack", "rabbit"));
    }
}
