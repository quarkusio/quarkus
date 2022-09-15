package io.quarkus.resteasy.jsonb;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@Path("/test")
public class ResourceSendingJsonObjects {

    @GET
    @Path("/objects")
    public List<JsonObject> getJsonObjects() {
        return Arrays.asList(new JsonObject().put("name", "bob").put("kind", "cat"),
                new JsonObject().put("name", "titi").put("kind", "bird"));
    }

    @GET
    @Path("/arrays")
    public List<JsonArray> getJsonArrays() {
        return Collections.singletonList(
                new JsonArray()
                        .add(new JsonObject().put("name", "bob").put("kind", "cat"))
                        .add(new JsonObject().put("name", "titi").put("kind", "bird")));
    }

    @POST
    @Path("/objects")
    public Response receiveJsonObjects(List<JsonObject> objects) {
        Assertions.assertEquals(2, objects.size());
        Assertions.assertEquals("bob", objects.get(0).getString("name"));
        Assertions.assertEquals("cat", objects.get(0).getString("kind"));
        Assertions.assertEquals("titi", objects.get(1).getString("name"));
        Assertions.assertEquals("bird", objects.get(1).getString("kind"));
        return Response.noContent().build();
    }

    @POST
    @Path("/arrays")
    public Response receiveJsonArrays(List<JsonArray> array) {
        Assertions.assertEquals(1, array.size());
        JsonArray objects = array.get(0);
        Assertions.assertEquals(2, objects.size());
        Assertions.assertEquals("bob", objects.getJsonObject(0).getString("name"));
        Assertions.assertEquals("cat", objects.getJsonObject(0).getString("kind"));
        Assertions.assertEquals("titi", objects.getJsonObject(1).getString("name"));
        Assertions.assertEquals("bird", objects.getJsonObject(1).getString("kind"));
        return Response.noContent().build();
    }
}
