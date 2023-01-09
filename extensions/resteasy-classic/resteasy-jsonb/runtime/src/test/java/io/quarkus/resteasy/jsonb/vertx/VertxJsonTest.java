package io.quarkus.resteasy.jsonb.vertx;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

class VertxJsonTest {

    private Jsonb jsonb;

    @BeforeEach
    public void setup() {
        JsonbConfig config = new JsonbConfig()
                .withSerializers(new VertxJson.JsonObjectSerializer(), new VertxJson.JsonArraySerializer())
                .withDeserializers(new VertxJson.JsonObjectDeserializer(), new VertxJson.JsonArrayDeserializer());
        jsonb = JsonbBuilder.create(config);
    }

    @AfterEach
    public void cleanup() throws Exception {
        jsonb.close();
    }

    public enum MyEnum {
        VALUE
    }

    @Test
    public void testSerializationOfJsonObject() {
        Instant instant = Instant.now();
        JsonObject object = new JsonObject()
                .put("string", "value")
                .put("enum", MyEnum.VALUE)
                .put("long", 1000000L)
                .put("float", 24.2f)
                .put("int", 2)
                .put("true", true)
                .put("false", false)
                .put("binary", "hello".getBytes())
                .put("instant", instant)
                .putNull("null");

        String serialized = jsonb.toJson(object);
        JsonObject json = jsonb.fromJson(serialized, JsonObject.class);
        assertJsonObject(instant, json);
    }

    @Test
    public void testSerializationAndDeserialization() {
        Instant instant = Instant.now();
        JsonArray array = new JsonArray()
                .add("s").add(MyEnum.VALUE).add(2222222L).add(21.3f).add(12).add(55.55).add(true).add(false)
                .add("hello".getBytes())
                .add(instant)
                .add(new JsonObject().put("hello", "world")).add(new JsonArray().add(1).add(2).add("3"));

        JsonObject json2 = new JsonObject()
                .put("sub", new JsonObject().put("hello", "world").put("int", 3))
                .put("array", array);

        List<JsonObject> list = new ArrayList<>();
        list.add(new JsonObject().put("key", "value"));
        list.add(json2);

        String json = jsonb.toJson(list);

        JsonArray deserialized = jsonb.fromJson(json, JsonArray.class);
        Assertions.assertEquals(2, deserialized.size());
        JsonObject one = deserialized.getJsonObject(0);
        Assertions.assertEquals("value", one.getString("key"));

        JsonObject two = deserialized.getJsonObject(1);
        Assertions.assertEquals(2, two.getJsonObject("sub").size());
        Assertions.assertEquals("world", two.getJsonObject("sub").getString("hello"));
        Assertions.assertEquals(3, two.getJsonObject("sub").getInteger("int"));

        JsonArray deserializedArray = two.getJsonArray("array");
        Assertions.assertEquals("s", deserializedArray.getString(0));
        Assertions.assertEquals(MyEnum.VALUE.name(), deserializedArray.getString(1));
        Assertions.assertEquals(2222222L, deserializedArray.getLong(2));
        Assertions.assertEquals(21.3f, deserializedArray.getFloat(3));
        Assertions.assertEquals(12, deserializedArray.getInteger(4));
        Assertions.assertEquals(55.55, deserializedArray.getDouble(5));
        Assertions.assertTrue(deserializedArray.getBoolean(6));
        Assertions.assertFalse(deserializedArray.getBoolean(7));
        Assertions.assertArrayEquals("hello".getBytes(), deserializedArray.getBinary(8));
        Assertions.assertEquals(instant, deserializedArray.getInstant(9));
        Assertions.assertEquals("world", deserializedArray.getJsonObject(10).getString("hello"));
        Assertions.assertEquals(1, deserializedArray.getJsonArray(11).getInteger(0));
        Assertions.assertEquals(2, deserializedArray.getJsonArray(11).getInteger(1));
        Assertions.assertEquals("3", deserializedArray.getJsonArray(11).getString(2));
    }

    @Test
    public void testWithEmptyObject() {
        JsonObject json = new JsonObject();
        String s = jsonb.toJson(json);
        Assertions.assertEquals("{}", s);
        JsonObject object = jsonb.fromJson(s, JsonObject.class);
        Assertions.assertTrue(object.isEmpty());
    }

    @Test
    public void testWithEmptyArray() {
        JsonArray json = new JsonArray();
        String s = jsonb.toJson(json);
        Assertions.assertEquals("[]", s);
        JsonArray object = jsonb.fromJson(s, JsonArray.class);
        Assertions.assertTrue(object.isEmpty());
    }

    private void assertJsonObject(Instant instant, JsonObject one) {
        Assertions.assertEquals("value", one.getString("string"));
        Assertions.assertEquals(MyEnum.VALUE.name(), one.getString("enum"));
        Assertions.assertEquals(1000000L, one.getLong("long"));
        Assertions.assertEquals(24.2f, one.getFloat("float"));
        Assertions.assertEquals(2, one.getInteger("int"));
        Assertions.assertTrue(one.getBoolean("true"));
        Assertions.assertFalse(one.getBoolean("false"));
        byte[] binaries = one.getBinary("binary");
        Assertions.assertArrayEquals("hello".getBytes(), binaries);
        Assertions.assertEquals(instant, one.getInstant("instant"));
        Assertions.assertNull(one.getValue("null"));
    }

}
