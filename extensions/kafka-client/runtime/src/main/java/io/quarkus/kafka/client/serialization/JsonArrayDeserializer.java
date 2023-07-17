package io.quarkus.kafka.client.serialization;

import org.apache.kafka.common.serialization.Deserializer;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;

/**
 * Kafka deserializer for raw bytes in a buffer
 */
public class JsonArrayDeserializer implements Deserializer<JsonArray> {

    @Override
    public JsonArray deserialize(String topic, byte[] data) {
        if (data == null)
            return null;

        return Buffer.buffer(data).toJsonArray();
    }

}
