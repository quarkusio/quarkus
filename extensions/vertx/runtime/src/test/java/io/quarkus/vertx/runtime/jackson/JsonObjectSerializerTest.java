package io.quarkus.vertx.runtime.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class JsonObjectSerializerTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test-module");
        module.addSerializer(JsonObject.class, new JsonObjectSerializer());
        module.addSerializer(JsonArray.class, new JsonArraySerializer());
        mapper.registerModule(module);
    }

    @Test
    public void serializeSimpleJsonObject() throws Exception {
        JsonObject obj = new JsonObject().put("name", "quarkus").put("version", 3);
        String json = mapper.writeValueAsString(obj);
        assertThat(json).contains("\"name\":\"quarkus\"");
        assertThat(json).contains("\"version\":3");
    }

    @Test
    public void serializeEmptyJsonObject() throws Exception {
        JsonObject obj = new JsonObject();
        String json = mapper.writeValueAsString(obj);
        assertThat(json).isEqualTo("{}");
    }

    @Test
    public void serializeNestedJsonObject() throws Exception {
        JsonObject obj = new JsonObject()
                .put("outer", new JsonObject().put("inner", "value"));
        String json = mapper.writeValueAsString(obj);
        // Jackson will serialize the inner map as a regular map since we only registered
        // the JsonObject serializer at the top level
        assertThat(json).contains("\"outer\"");
        assertThat(json).contains("\"inner\":\"value\"");
    }

    @Test
    public void serializeSimpleJsonArray() throws Exception {
        JsonArray arr = new JsonArray().add("a").add("b").add("c");
        String json = mapper.writeValueAsString(arr);
        assertThat(json).isEqualTo("[\"a\",\"b\",\"c\"]");
    }

    @Test
    public void serializeEmptyJsonArray() throws Exception {
        JsonArray arr = new JsonArray();
        String json = mapper.writeValueAsString(arr);
        assertThat(json).isEqualTo("[]");
    }

    @Test
    public void serializeJsonArrayWithMixedTypes() throws Exception {
        JsonArray arr = new JsonArray().add("text").add(42).add(true).addNull();
        String json = mapper.writeValueAsString(arr);
        assertThat(json).isEqualTo("[\"text\",42,true,null]");
    }

    @Test
    public void serializeJsonObjectWithJsonArray() throws Exception {
        JsonObject obj = new JsonObject()
                .put("items", new JsonArray().add(1).add(2).add(3));
        String json = mapper.writeValueAsString(obj);
        assertThat(json).contains("\"items\":[1,2,3]");
    }
}
