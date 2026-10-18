package io.quarkus.pulsar;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

import org.apache.pulsar.client.api.Schema;

import io.quarkus.pulsar.schema.BufferSchema;
import io.quarkus.pulsar.schema.JsonArraySchema;
import io.quarkus.pulsar.schema.JsonObjectSchema;
import io.quarkus.pulsar.schema.ObjectMapperSchema;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@Recorder
public class SchemaProviderRecorder {

    public <T> Supplier<Schema<T>> createJsonSchema(Class<T> type) {
        return new Supplier<Schema<T>>() {
            @Override
            public Schema<T> get() {
                return Schema.JSON(type);
            }
        };
    };

    public <T> Supplier<Schema<T>> createAvroSchema(Class<T> type) {
        return new Supplier<Schema<T>>() {
            @Override
            public Schema<T> get() {
                return Schema.AVRO(type);
            }
        };
    };

    public Supplier<Schema<?>> createProtoBufSchema(Class<?> type) {
        return new Supplier<Schema<?>>() {
            @Override
            public Schema<?> get() {
                return Schema.PROTOBUF((Class<com.google.protobuf.GeneratedMessageV3>) type);
            }
        };
    }

    public <T> RuntimeValue<Schema<T>> createObjectMapperSchema(Class<T> type) {
        return new RuntimeValue<>(ObjectMapperSchema.of(type));
    }

    public RuntimeValue<Schema<Buffer>> createBufferSchema() {
        return new RuntimeValue<>(BufferSchema.INSTANCE);
    }

    public RuntimeValue<Schema<JsonObject>> createJsonObjectSchema() {
        return new RuntimeValue<>(JsonObjectSchema.INSTANCE);
    }

    public RuntimeValue<Schema<JsonArray>> createJsonArraySchema() {
        return new RuntimeValue<>(JsonArraySchema.INSTANCE);
    }

    public RuntimeValue<Schema<ByteBuffer>> createByteBufferSchema() {
        return new RuntimeValue<>(Schema.BYTEBUFFER);
    }

}
