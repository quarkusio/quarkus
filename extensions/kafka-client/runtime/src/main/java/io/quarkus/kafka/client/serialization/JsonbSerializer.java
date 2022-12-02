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
    public static final String NULL_AS_NULL_CONFIG = "json.serialize.null-as-null";

    private final Jsonb jsonb;
    private final boolean jsonbNeedsClosing;

    private boolean nullAsNull = false;

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
        if (configs.containsKey(NULL_AS_NULL_CONFIG) && Boolean.parseBoolean((String) configs.get(NULL_AS_NULL_CONFIG))) {
            nullAsNull = true;
        }
    }

    @Override
    public byte[] serialize(String topic, T data) {
        if (nullAsNull && data == null) {
            return null;
        }

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
