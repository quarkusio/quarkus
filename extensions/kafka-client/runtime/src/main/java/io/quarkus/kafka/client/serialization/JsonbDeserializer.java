package io.quarkus.kafka.client.serialization;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.json.bind.Jsonb;

import org.apache.kafka.common.serialization.Deserializer;

/**
 * A {@link Deserializer} that deserializes JSON using JSON-B.
 */
public class JsonbDeserializer<T> implements Deserializer<T> {

    private final Jsonb jsonb;
    private final Class<T> type;
    private final boolean jsonbNeedsClosing;

    public JsonbDeserializer(Class<T> type) {
        this(type, JsonbProducer.get(), true);
    }

    public JsonbDeserializer(Class<T> type, Jsonb jsonb) {
        this(type, jsonb, false);
    }

    private JsonbDeserializer(Class<T> type, Jsonb jsonb, boolean jsonbNeedsClosing) {
        this.type = type;
        this.jsonb = jsonb;
        this.jsonbNeedsClosing = jsonbNeedsClosing;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try (InputStream is = new ByteArrayInputStream(data)) {
            return jsonb.fromJson(is, type);
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
