package io.quarkus.reactivemessaging.http.runtime.serializers;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

/**
 * Serializer for JsonObject
 */
public class JsonObjectSerializer implements Serializer<JsonObject> {
    @Override
    public boolean handles(Object payload) {
        return payload instanceof JsonObject;
    }

    @Override
    public Buffer serialize(JsonObject payload) {
        return payload.toBuffer();
    }
}
