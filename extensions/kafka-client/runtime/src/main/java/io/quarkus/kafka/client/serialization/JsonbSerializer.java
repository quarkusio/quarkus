package io.quarkus.kafka.client.serialization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.json.bind.Jsonb;

import org.apache.kafka.common.serialization.Serializer;

/**
 * A {@link Serializer} that serializes to JSON using JSON-B.
 */
public class JsonbSerializer<T> implements Serializer<T> {

    private final Jsonb jsonb;
    private final boolean jsonbNeedsClosing;

    public JsonbSerializer() {
        this(JsonbProducer.get(), true);
    }

    public JsonbSerializer(Jsonb jsonb) {
        this(jsonb, false);
    }

    private JsonbSerializer(Jsonb jsonb, boolean jsonbNeedsClosing) {
        this.jsonb = jsonb;
        this.jsonbNeedsClosing = jsonbNeedsClosing;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, T data) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            jsonb.toJson(data, output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        if (!jsonbNeedsClosing) {
            return;
        }

        try {
            jsonb.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
