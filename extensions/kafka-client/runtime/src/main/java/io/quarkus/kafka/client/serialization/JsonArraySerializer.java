package io.quarkus.kafka.client.serialization;

import org.apache.kafka.common.serialization.Serializer;

import io.vertx.core.json.JsonArray;

/**
 * Kafka serializer for raw bytes in a buffer
 */
public class JsonArraySerializer implements Serializer<JsonArray> {

    @Override
    public byte[] serialize(String topic, JsonArray data) {
        if (data == null)
            return null;

        return data.encode().getBytes();
    }

}
