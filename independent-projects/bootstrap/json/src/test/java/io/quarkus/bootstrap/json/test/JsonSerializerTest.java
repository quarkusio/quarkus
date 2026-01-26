package io.quarkus.bootstrap.json.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.json.Json;
import io.quarkus.bootstrap.json.Json.JsonArrayBuilder;
import io.quarkus.bootstrap.json.Json.JsonObjectBuilder;
import io.quarkus.bootstrap.json.JsonArray;
import io.quarkus.bootstrap.json.JsonBoolean;
import io.quarkus.bootstrap.json.JsonInteger;
import io.quarkus.bootstrap.json.JsonObject;
import io.quarkus.bootstrap.json.JsonReader;
import io.quarkus.bootstrap.json.JsonString;

class JsonSerializerTest {

    private String toJson(Json.JsonBuilder<?> builder) throws IOException {
        StringBuilder sb = new StringBuilder();
        builder.appendTo(sb);
        return sb.toString();
    }

    @Test
    void testSimpleObject() throws IOException {
        String json = toJson(Json.object()
                .put("name", "John")
                .put("age", 30));
        JsonObject parsed = JsonReader.of(json).read();
        assertEquals("John", ((JsonString) parsed.get("name")).value());
        assertEquals(30L, ((JsonInteger) parsed.get("age")).longValue());
    }

    @Test
    void testSimpleArray() throws IOException {
        String json = toJson(Json.array()
                .add("apple")
                .add("banana")
                .add("cherry"));
        assertEquals("[\"apple\",\"banana\",\"cherry\"]", json);
    }

    @Test
    void testNestedObjects() throws IOException {
        String json = toJson(Json.object()
                .put("user", Json.object()
                        .put("name", "Alice")
                        .put("email", "alice@example.com"))
                .put("active", true));
        JsonObject parsed = JsonReader.of(json).read();
        JsonObject user = (JsonObject) parsed.get("user");
        assertEquals("Alice", ((JsonString) user.get("name")).value());
        assertEquals("alice@example.com", ((JsonString) user.get("email")).value());
        assertTrue(((JsonBoolean) parsed.get("active")).value());
    }

    @Test
    void testNestedArrays() throws IOException {
        String json = toJson(Json.array()
                .add(Json.array().add(1).add(2))
                .add(Json.array().add(3).add(4)));
        assertEquals("[[1,2],[3,4]]", json);
    }

    @Test
    void testMixedTypes() throws IOException {
        String json = toJson(Json.object()
                .put("string", "value")
                .put("integer", 42)
                .put("long", 9876543210L)
                .put("boolean", false));
        JsonObject parsed = JsonReader.of(json).read();
        assertEquals("value", ((JsonString) parsed.get("string")).value());
        assertEquals(42L, ((JsonInteger) parsed.get("integer")).longValue());
        assertEquals(9876543210L, ((JsonInteger) parsed.get("long")).longValue());
        assertFalse(((JsonBoolean) parsed.get("boolean")).value());
    }

    @Test
    void testEmptyObject() throws IOException {
        String json = toJson(Json.object());
        assertEquals("{}", json);
    }

    @Test
    void testEmptyArray() throws IOException {
        String json = toJson(Json.array());
        assertEquals("[]", json);
    }

    @Test
    void testStringEscapingQuotes() throws IOException {
        String json = toJson(Json.object()
                .put("message", "He said \"Hello\""));
        assertEquals("{\"message\":\"He said \\\"Hello\\\"\"}", json);
    }

    @Test
    void testStringEscapingBackslash() throws IOException {
        String json = toJson(Json.object()
                .put("path", "C:\\Users\\John"));
        assertEquals("{\"path\":\"C:\\\\Users\\\\John\"}", json);
    }

    @Test
    void testStringEscapingControlCharacters() throws IOException {
        String json = toJson(Json.object()
                .put("text", "Line1\nLine2\tTabbed\rReturn"));
        assertEquals("{\"text\":\"Line1\\u000aLine2\\u0009Tabbed\\u000dReturn\"}", json);
    }

    @Test
    void testStringEscapingAllControlCharacters() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= 0x1f; i++) {
            sb.append((char) i);
        }
        String json = toJson(Json.object()
                .put("controls", sb.toString()));

        String expected = "{\"controls\":\"\\u0000\\u0001\\u0002\\u0003\\u0004\\u0005\\u0006\\u0007" +
                "\\u0008\\u0009\\u000a\\u000b\\u000c\\u000d\\u000e\\u000f" +
                "\\u0010\\u0011\\u0012\\u0013\\u0014\\u0015\\u0016\\u0017" +
                "\\u0018\\u0019\\u001a\\u001b\\u001c\\u001d\\u001e\\u001f\"}";
        assertEquals(expected, json);
    }

    @Test
    void testStringWithSpecialCharacters() throws IOException {
        String json = toJson(Json.object()
                .put("special", "äöü ñ €"));
        assertEquals("{\"special\":\"äöü ñ €\"}", json);
    }

    @Test
    void testEmptyString() throws IOException {
        String json = toJson(Json.object()
                .put("empty", ""));
        assertEquals("{\"empty\":\"\"}", json);
    }

    @Test
    void testArrayWithMixedTypes() throws IOException {
        String json = toJson(Json.array()
                .add("text")
                .add(123)
                .add(true)
                .add(Json.object().put("key", "value")));
        assertEquals("[\"text\",123,true,{\"key\":\"value\"}]", json);
    }

    @Test
    void testComplexNestedStructure() throws IOException {
        String json = toJson(Json.object()
                .put("users", Json.array()
                        .add(Json.object()
                                .put("id", 1)
                                .put("name", "Alice"))
                        .add(Json.object()
                                .put("id", 2)
                                .put("name", "Bob")))
                .put("count", 2));
        JsonObject parsed = JsonReader.of(json).read();
        JsonArray users = (JsonArray) parsed.get("users");
        assertEquals(2, users.size());
        JsonObject alice = (JsonObject) users.value().get(0);
        assertEquals(1L, ((JsonInteger) alice.get("id")).longValue());
        assertEquals("Alice", ((JsonString) alice.get("name")).value());
        JsonObject bob = (JsonObject) users.value().get(1);
        assertEquals(2L, ((JsonInteger) bob.get("id")).longValue());
        assertEquals("Bob", ((JsonString) bob.get("name")).value());
        assertEquals(2L, ((JsonInteger) parsed.get("count")).longValue());
    }

    @Test
    void testLongNumbers() throws IOException {
        String json = toJson(Json.object()
                .put("maxInt", Integer.MAX_VALUE)
                .put("minInt", Integer.MIN_VALUE)
                .put("maxLong", Long.MAX_VALUE)
                .put("minLong", Long.MIN_VALUE));
        JsonObject parsed = JsonReader.of(json).read();
        assertEquals(Integer.MAX_VALUE, ((JsonInteger) parsed.get("maxInt")).longValue());
        assertEquals(Integer.MIN_VALUE, ((JsonInteger) parsed.get("minInt")).longValue());
        assertEquals(Long.MAX_VALUE, ((JsonInteger) parsed.get("maxLong")).longValue());
        assertEquals(Long.MIN_VALUE, ((JsonInteger) parsed.get("minLong")).longValue());
    }

    @Test
    void testStringWithQuotesAndBackslashes() throws IOException {
        String json = toJson(Json.object()
                .put("complex", "\"\\\"\\\\\""));
        assertEquals("{\"complex\":\"\\\"\\\\\\\"\\\\\\\\\\\"\"}", json);
    }

    @Test
    void testUnicodeCharacters() throws IOException {
        String json = toJson(Json.object()
                .put("emoji", "Hello 🌍"));
        assertEquals("{\"emoji\":\"Hello 🌍\"}", json);
    }

    @Disabled("https://github.com/quarkusio/quarkus/issues/52196")
    @Test
    void testFluentApiIncompatibility() throws IOException {
        JsonObjectBuilder obj = Json.object();
        // calls JsonObjectBuilder#put(String, Object) and results in {"bar":null} instead of {"bar":{"val":30}}
        obj.put("bar", Json.object().put("val", Long.valueOf(30)));
        JsonArrayBuilder arr = Json.array();
        // calls JsonArrayBuilder#add(Object) and results in {"foo":true} instead of {"foo":[30]}
        obj.put("foo", arr.add(Long.valueOf(30)));
        assertEquals("{\"bar\":{\"val\":30},\"foo\":[30]}", toJson(obj));
    }
}
