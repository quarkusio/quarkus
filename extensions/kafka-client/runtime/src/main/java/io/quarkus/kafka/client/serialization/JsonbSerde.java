package io.quarkus.kafka.client.serialization;

import java.util.Map;

import jakarta.json.bind.Jsonb;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

/**
 * A {@link Serde} that (de-)serializes JSON using JSON-B.
 */
public class JsonbSerde<T> implements Serde<T> {

    private final Jsonb jsonb;
    private final boolean jsonbNeedsClosing;

    private final JsonbSerializer<T> serializer;
    private final JsonbDeserializer<T> deserializer;

    public JsonbSerde(Class<T> type) {
        this(type, JsonbProducer.get(), true);
    }

    public JsonbSerde(Class<T> type, Jsonb jsonb) {
        this(type, jsonb, false);
    }

    private JsonbSerde(Class<T> type, Jsonb jsonb, boolean jsonbNeedsClosing) {
        this.jsonb = jsonb;
        this.jsonbNeedsClosing = jsonbNeedsClosing;

        this.serializer = new JsonbSerializer<T>(jsonb);
        this.deserializer = new JsonbDeserializer<T>(type, jsonb);
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public void close() {
        serializer.close();
        deserializer.close();

        if (jsonbNeedsClosing) {
            try {
                jsonb.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Serializer<T> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<T> deserializer() {
        return deserializer;
    }
}
