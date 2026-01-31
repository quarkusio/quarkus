package io.quarkus.bootstrap.json.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.json.Json;
import io.quarkus.bootstrap.json.JsonArray;
import io.quarkus.bootstrap.json.JsonBoolean;
import io.quarkus.bootstrap.json.JsonInteger;
import io.quarkus.bootstrap.json.JsonObject;
import io.quarkus.bootstrap.json.JsonReader;
import io.quarkus.bootstrap.json.JsonString;

class JsonRoundTripTest {

    private String toJson(Json.JsonBuilder<?> builder) throws IOException {
        StringBuilder sb = new StringBuilder();
        builder.appendTo(sb);
        return sb.toString();
    }

    @Test
    void testSimpleObjectRoundTrip() throws IOException {
        Json.JsonObjectBuilder builder = Json.object()
                .put("name", "John")
                .put("age", 30);

        String json = toJson(builder);
        JsonObject parsed = JsonReader.of(json).read();
        String jsonAgain = toJson(Json.object()
                .put("name", ((JsonString) parsed.get("name")).value())
                .put("age", (int) ((JsonInteger) parsed.get("age")).longValue()));

        assertEquals(json, jsonAgain);
    }

    @Test
    void testSimpleArrayRoundTrip() throws IOException {
        Json.JsonArrayBuilder builder = Json.array()
                .add("apple")
                .add("banana")
                .add("cherry");

        String json = toJson(builder);
        JsonArray parsed = JsonReader.of(json).read();
        String jsonAgain = toJson(Json.array()
                .add(((JsonString) parsed.value().get(0)).value())
                .add(((JsonString) parsed.value().get(1)).value())
                .add(((JsonString) parsed.value().get(2)).value()));

        assertEquals(json, jsonAgain);
    }

    @Test
    void testStringEscapingRoundTrip() throws IOException {
        String original = "He said \"Hello\" and used \\ backslash";
        Json.JsonObjectBuilder builder = Json.object()
                .put("message", original);

        String json = toJson(builder);
        JsonObject parsed = JsonReader.of(json).read();
        String extracted = ((JsonString) parsed.get("message")).value();

        assertEquals(original, extracted);
    }

    @Test
    void testControlCharactersRoundTrip() throws IOException {
        String original = "Line1\nLine2\tTabbed\rReturn";
        Json.JsonObjectBuilder builder = Json.object()
                .put("text", original);

        String json = toJson(builder);
        JsonObject parsed = JsonReader.of(json).read();
        String extracted = ((JsonString) parsed.get("text")).value();

        assertEquals(original, extracted);
    }

    @Test
    void testAllControlCharactersRoundTrip() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= 0x1f; i++) {
            sb.append((char) i);
        }
        String original = sb.toString();

        Json.JsonObjectBuilder builder = Json.object()
                .put("controls", original);

        String json = toJson(builder);
        JsonObject parsed = JsonReader.of(json).read();
        String extracted = ((JsonString) parsed.get("controls")).value();

        assertEquals(original, extracted);
    }

    @Test
    void testSpecialCharactersRoundTrip() throws IOException {
        String original = "äöü ñ € 🌍";
        Json.JsonObjectBuilder builder = Json.object()
                .put("special", original);

        String json = toJson(builder);
        JsonObject parsed = JsonReader.of(json).read();
        String extracted = ((JsonString) parsed.get("special")).value();

        assertEquals(original, extracted);
    }

    @Test
    void testEmptyStringRoundTrip() throws IOException {
        String original = "";
        Json.JsonObjectBuilder builder = Json.object()
                .put("empty", original);

        String json = toJson(builder);
        JsonObject parsed = JsonReader.of(json).read();
        String extracted = ((JsonString) parsed.get("empty")).value();

        assertEquals(original, extracted);
    }

    @Test
    void testNumbersRoundTrip() throws IOException {
        Json.JsonObjectBuilder builder = Json.object()
                .put("int", 42)
                .put("long", 9876543210L)
                .put("maxInt", Integer.MAX_VALUE)
                .put("minInt", Integer.MIN_VALUE)
                .put("maxLong", Long.MAX_VALUE)
                .put("minLong", Long.MIN_VALUE);

        String json = toJson(builder);
        JsonObject parsed = JsonReader.of(json).read();

        String jsonAgain = toJson(Json.object()
                .put("int", (int) ((JsonInteger) parsed.get("int")).longValue())
                .put("long", ((JsonInteger) parsed.get("long")).longValue())
                .put("maxInt", (int) ((JsonInteger) parsed.get("maxInt")).longValue())
                .put("minInt", (int) ((JsonInteger) parsed.get("minInt")).longValue())
                .put("maxLong", ((JsonInteger) parsed.get("maxLong")).longValue())
                .put("minLong", ((JsonInteger) parsed.get("minLong")).longValue()));

        assertEquals(json, jsonAgain);
    }

    @Test
    void testBooleanRoundTrip() throws IOException {
        Json.JsonObjectBuilder builder = Json.object()
                .put("trueVal", true)
                .put("falseVal", false);

        String json = toJson(builder);
        JsonObject parsed = JsonReader.of(json).read();

        String jsonAgain = toJson(Json.object()
                .put("trueVal", ((JsonBoolean) parsed.get("trueVal")).value())
                .put("falseVal", ((JsonBoolean) parsed.get("falseVal")).value()));

        assertEquals(json, jsonAgain);
    }

    @Test
    void testNestedStructureRoundTrip() throws IOException {
        Json.JsonObjectBuilder builder = Json.object()
                .put("user", Json.object()
                        .put("name", "Alice")
                        .put("age", 25))
                .put("active", true);

        String json = toJson(builder);
        JsonObject parsed = JsonReader.of(json).read();

        JsonObject user = (JsonObject) parsed.get("user");
        String jsonAgain = toJson(Json.object()
                .put("user", Json.object()
                        .put("name", ((JsonString) user.get("name")).value())
                        .put("age", (int) ((JsonInteger) user.get("age")).longValue()))
                .put("active", ((JsonBoolean) parsed.get("active")).value()));

        assertEquals(json, jsonAgain);
    }

    @Test
    void testComplexNestedRoundTrip() throws IOException {
        Json.JsonObjectBuilder builder = Json.object()
                .put("users", Json.array()
                        .add(Json.object()
                                .put("id", 1)
                                .put("name", "Alice"))
                        .add(Json.object()
                                .put("id", 2)
                                .put("name", "Bob")))
                .put("count", 2);

        String json = toJson(builder);
        JsonObject parsed = JsonReader.of(json).read();

        JsonArray users = (JsonArray) parsed.get("users");
        JsonObject alice = (JsonObject) users.value().get(0);
        JsonObject bob = (JsonObject) users.value().get(1);

        String jsonAgain = toJson(Json.object()
                .put("users", Json.array()
                        .add(Json.object()
                                .put("id", (int) ((JsonInteger) alice.get("id")).longValue())
                                .put("name", ((JsonString) alice.get("name")).value()))
                        .add(Json.object()
                                .put("id", (int) ((JsonInteger) bob.get("id")).longValue())
                                .put("name", ((JsonString) bob.get("name")).value())))
                .put("count", (int) ((JsonInteger) parsed.get("count")).longValue()));

        assertEquals(json, jsonAgain);
    }

    @Test
    void testMixedArrayRoundTrip() throws IOException {
        Json.JsonArrayBuilder builder = Json.array()
                .add("text")
                .add(123)
                .add(true)
                .add(Json.object().put("key", "value"));

        String json = toJson(builder);
        JsonArray parsed = JsonReader.of(json).read();

        JsonObject obj = (JsonObject) parsed.value().get(3);
        String jsonAgain = toJson(Json.array()
                .add(((JsonString) parsed.value().get(0)).value())
                .add((int) ((JsonInteger) parsed.value().get(1)).longValue())
                .add(((JsonBoolean) parsed.value().get(2)).value())
                .add(Json.object().put("key", ((JsonString) obj.get("key")).value())));

        assertEquals(json, jsonAgain);
    }

    @Test
    void testQuotesAndBackslashesRoundTrip() throws IOException {
        String original = "\"\\\"\\\\\"";
        Json.JsonObjectBuilder builder = Json.object()
                .put("complex", original);

        String json = toJson(builder);
        JsonObject parsed = JsonReader.of(json).read();
        String extracted = ((JsonString) parsed.get("complex")).value();

        assertEquals(original, extracted);
    }

    @Test
    void testMultipleRoundTrips() throws IOException {
        String original = "Test with \"quotes\", \\backslashes\\ and\ncontrol\tchars";
        Json.JsonObjectBuilder builder = Json.object()
                .put("text", original);

        String json1 = toJson(builder);
        JsonObject parsed1 = JsonReader.of(json1).read();
        String extracted1 = ((JsonString) parsed1.get("text")).value();
        assertEquals(original, extracted1);

        String json2 = toJson(Json.object().put("text", extracted1));
        JsonObject parsed2 = JsonReader.of(json2).read();
        String extracted2 = ((JsonString) parsed2.get("text")).value();
        assertEquals(original, extracted2);

        String json3 = toJson(Json.object().put("text", extracted2));
        JsonObject parsed3 = JsonReader.of(json3).read();
        String extracted3 = ((JsonString) parsed3.get("text")).value();
        assertEquals(original, extracted3);

        assertEquals(json1, json2);
        assertEquals(json2, json3);
    }
}
