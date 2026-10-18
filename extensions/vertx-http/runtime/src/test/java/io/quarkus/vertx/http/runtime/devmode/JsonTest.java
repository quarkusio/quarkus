package io.quarkus.vertx.http.runtime.devmode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import io.quarkus.vertx.http.runtime.devmode.Json.JsonArrayBuilder;
import io.quarkus.vertx.http.runtime.devmode.Json.JsonObjectBuilder;

public class JsonTest {

    @Test
    public void testJsonArray() {
        assertEquals("[\"foo\",\"bar\",[\"baz\"]]", Json.array().add("foo").add("bar").add(Json.array().add("baz")).build());
    }

    @Test
    public void testJsonObject() {
        assertEquals("{\"foo\":\"bar\",\"baz\":[\"qux\"]}",
                Json.object().put("foo", "bar").put("baz", Json.array().add("qux")).build());
    }

    @Test
    public void testIgnoreEmptyBuilders() {
        assertEquals("[true]", Json.array(true).add(true).add(Json.object(true).put("foo", Json.object())).build());
        JsonObjectBuilder objectBuilder = Json.object();
        JsonArrayBuilder arrayBuilder = Json.array().add(objectBuilder);
        objectBuilder.put("foo", "bar");
        assertEquals("[{\"foo\":\"bar\"}]", arrayBuilder.build());
    }

    @Test
    public void testABitMoreComplexStructure() {
        JsonObjectBuilder builder = Json.object().put("items", Json.array().add(1).add(2)).put("name", "Foo").put("parent",
                Json.object(true).put("name", "Martin").put("age", 100).put("active", true).put("children",
                        Json.array(true).add(Json.object())));
        assertFalse(builder.isEmpty());
        assertEquals("{\"items\":[1,2],\"name\":\"Foo\",\"parent\":{\"name\":\"Martin\",\"age\":100,\"active\":true}}",
                builder.build());
    }

    @Test
    public void testEscaping() {
        assertEquals("{\"foo\":\"bar=\\\"baz\\u000a and \\u0009 F\\\"\"}",
                Json.object().put("foo", "bar=\"baz\n and \t F\"").build());
    }

}
