package io.quarkus.resteasy.reactive.jsonb.common.runtime.serialisers;

import java.lang.reflect.Type;

import jakarta.json.JsonNumber;
import jakarta.json.JsonValue;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Provides Vert.x JSON Object/Array <-> JsonB serializers and deserializers.
 *
 * These are useful when you use a List of JsonObject or JsonArray, as the serialization use JSON-B to serializer the
 * contained items.
 */
public final class VertxJson {

    private VertxJson() {

    }

    public static void copy(JsonObject object, jakarta.json.JsonObject origin) {
        origin.keySet().forEach(key -> {
            JsonValue value = origin.get(key);
            JsonValue.ValueType kind = value.getValueType();

            switch (kind) {
                case STRING:
                    object.put(key, origin.getString(key));
                    break;
                case NULL:
                    object.putNull(key);
                    break;
                case TRUE:
                    object.put(key, true);
                    break;
                case FALSE:
                    object.put(key, false);
                    break;
                case NUMBER:
                    JsonNumber number = origin.getJsonNumber(key);
                    if (number.isIntegral()) {
                        object.put(key, number.longValue());
                    } else {
                        object.put(key, number.doubleValue());
                    }
                    break;
                case ARRAY:
                    JsonArray array = new JsonArray();
                    copy(array, origin.getJsonArray(key));
                    object.put(key, array);
                    break;
                case OBJECT:
                    JsonObject json = new JsonObject();
                    copy(json, origin.getJsonObject(key));
                    object.put(key, json);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown JSON Value " + kind);
            }
        });
    }

    public static void copy(JsonArray array, jakarta.json.JsonArray origin) {
        for (int i = 0; i < origin.size(); i++) {
            JsonValue value = origin.get(i);
            JsonValue.ValueType kind = value.getValueType();
            switch (kind) {
                case STRING:
                    array.add(origin.getString(i));
                    break;
                case TRUE:
                    array.add(true);
                    break;
                case FALSE:
                    array.add(false);
                    break;
                case NULL:
                    array.addNull();
                    break;
                case NUMBER:
                    JsonNumber number = origin.getJsonNumber(i);
                    if (number.isIntegral()) {
                        array.add(number.longValue());
                    } else {
                        array.add(number.doubleValue());
                    }
                    break;
                case ARRAY:
                    JsonArray newArray = new JsonArray();
                    copy(newArray, origin.getJsonArray(i));
                    array.add(newArray);
                    break;
                case OBJECT:
                    JsonObject newObject = new JsonObject();
                    copy(newObject, origin.getJsonObject(i));
                    array.add(newObject);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown JSON Value " + kind);
            }
        }
    }

    public static class JsonObjectSerializer implements JsonbSerializer<JsonObject> {
        @Override
        public void serialize(JsonObject json, JsonGenerator generator, SerializationContext ctxt) {
            ctxt.serialize(json.getMap(), generator);
        }
    }

    public static class JsonArrayDeserializer implements JsonbDeserializer<JsonArray> {

        @Override
        public JsonArray deserialize(JsonParser parser, DeserializationContext context, Type type) {
            JsonArray object = new JsonArray();
            copy(object, parser.getArray());
            return object;
        }
    }

    public static class JsonArraySerializer implements JsonbSerializer<JsonArray> {
        @Override
        public void serialize(JsonArray json, JsonGenerator generator, SerializationContext ctxt) {
            ctxt.serialize(json.getList(), generator);
        }
    }

    public static class JsonObjectDeserializer implements JsonbDeserializer<JsonObject> {

        @Override
        public JsonObject deserialize(JsonParser parser, DeserializationContext context, Type type) {
            JsonObject object = new JsonObject();
            copy(object, parser.getObject());
            return object;
        }
    }
}
