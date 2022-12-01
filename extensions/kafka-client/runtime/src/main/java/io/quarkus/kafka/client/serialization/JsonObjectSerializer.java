package io.quarkus.kafka.client.serialization;

import org.apache.kafka.common.serialization.Serializer;

import io.vertx.core.json.JsonObject;

/**
 * Kafka serializer for raw bytes in a buffer
 */
public class JsonObjectSerializer implements Serializer<JsonObject> {

    @Override
    public byte[] serialize(String topic, JsonObject data) {
        if (data == null)
            return null;

        return data.encode().getBytes();
    }

}
