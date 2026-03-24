package io.quarkus.vertx.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.Message;

/**
 * Verifies that JSON types (JsonObject, JsonArray) and POJOs can be sent over the event bus
 * and properly round-tripped through the Quarkus Jackson codec integration.
 */
public class JsonCodecIntegrationTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(JsonConsumers.class));

    @Inject
    Vertx vertx;

    @Test
    public void testJsonObjectRoundTrip() {
        JsonObject input = new JsonObject()
                .put("name", "quarkus")
                .put("version", 5);

        JsonObject result = vertx.eventBus().<JsonObject> request("json-object", input)
                .onItem().transform(Message::body)
                .await().indefinitely();

        assertNotNull(result);
        assertEquals("QUARKUS", result.getString("name"));
        assertEquals(5, result.getInteger("version"));
    }

    @Test
    public void testJsonArrayRoundTrip() {
        JsonArray input = new JsonArray().add("a").add("b").add("c");

        JsonArray result = vertx.eventBus().<JsonArray> request("json-array", input)
                .onItem().transform(Message::body)
                .await().indefinitely();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("A", result.getString(0));
        assertEquals("B", result.getString(1));
        assertEquals("C", result.getString(2));
    }

    @Test
    public void testJsonObjectWithNestedTypes() {
        Instant now = Instant.now();
        JsonObject input = new JsonObject()
                .put("timestamp", now.toString())
                .put("data", new JsonObject().put("key", "value"))
                .put("items", new JsonArray().add(1).add(2).add(3));

        JsonObject result = vertx.eventBus().<JsonObject> request("json-nested", input)
                .onItem().transform(Message::body)
                .await().indefinitely();

        assertNotNull(result);
        assertEquals(now.toString(), result.getString("timestamp"));
        assertEquals("value", result.getJsonObject("data").getString("key"));
        assertEquals(3, result.getJsonArray("items").size());
        assertEquals(true, result.getBoolean("processed"));
    }

    @Test
    public void testPojoViaJsonObject() {
        // Send a JsonObject that represents a POJO-like structure and get a JsonObject back
        JsonObject input = new JsonObject()
                .put("firstName", "John")
                .put("lastName", "Doe");

        String result = vertx.eventBus().<String> request("json-pojo", input)
                .onItem().transform(Message::body)
                .await().indefinitely();

        assertEquals("Hello John Doe", result);
    }

    @Test
    public void testJsonObjectWithCompletionStage() {
        JsonObject input = new JsonObject().put("greeting", "hello");

        JsonObject result = vertx.eventBus().<JsonObject> request("json-cs", input)
                .onItem().transform(Message::body)
                .await().indefinitely();

        assertNotNull(result);
        assertEquals("HELLO", result.getString("greeting"));
    }

    @Test
    public void testJsonObjectWithUni() {
        JsonObject input = new JsonObject().put("greeting", "world");

        JsonObject result = vertx.eventBus().<JsonObject> request("json-uni", input)
                .onItem().transform(Message::body)
                .await().indefinitely();

        assertNotNull(result);
        assertEquals("WORLD", result.getString("greeting"));
    }

    static class JsonConsumers {

        @ConsumeEvent("json-object")
        JsonObject processJsonObject(JsonObject input) {
            return new JsonObject()
                    .put("name", input.getString("name").toUpperCase())
                    .put("version", input.getInteger("version"));
        }

        @ConsumeEvent("json-array")
        JsonArray processJsonArray(JsonArray input) {
            JsonArray result = new JsonArray();
            for (int i = 0; i < input.size(); i++) {
                result.add(input.getString(i).toUpperCase());
            }
            return result;
        }

        @ConsumeEvent("json-nested")
        JsonObject processNested(JsonObject input) {
            return input.copy().put("processed", true);
        }

        @ConsumeEvent("json-pojo")
        String processPojo(JsonObject input) {
            return "Hello " + input.getString("firstName") + " " + input.getString("lastName");
        }

        @ConsumeEvent("json-cs")
        CompletionStage<JsonObject> processWithCompletionStage(JsonObject input) {
            JsonObject result = new JsonObject()
                    .put("greeting", input.getString("greeting").toUpperCase());
            return CompletableFuture.completedFuture(result);
        }

        @ConsumeEvent("json-uni")
        Uni<JsonObject> processWithUni(JsonObject input) {
            JsonObject result = new JsonObject()
                    .put("greeting", input.getString("greeting").toUpperCase());
            return Uni.createFrom().item(result);
        }
    }
}
